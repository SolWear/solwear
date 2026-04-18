package com.example.solvare.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solvare.data.NetworkModule
import android.util.Base64
import com.example.solvare.nfc.NdefProtocol
import com.example.solvare.nfc.NfcMode
import com.example.solvare.nfc.NfcResult
import com.example.solvare.nfc.NfcSessionManager
import com.example.solvare.nfc.NfcState
import com.example.solvare.util.Base58
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WalletUiState(
    val pubkey: String? = null,
    val balanceSol: Double? = null,
    val lastConnectedAtMillis: Long? = null,
    val priceUsd: Double? = null,
    val isLoadingBalance: Boolean = false,
    val balanceError: String? = null,
    val sendRecipient: String = "",
    val sendAmount: String = "",
    val sendError: String? = null,
    val recipientError: String? = null,
    val amountError: String? = null,
    val txSignature: String? = null,
    val txStatus: TxStatus = TxStatus.IDLE,
    val showNfcSheet: Boolean = false,
    val nfcPurpose: NfcPurpose = NfcPurpose.READ_WALLET
)

enum class TxStatus { IDLE, BUILDING, AWAITING_SIGN, SENDING, CONFIRMING, CONFIRMED, ERROR }
enum class NfcPurpose { READ_WALLET, SIGN_TRANSACTION }

class WalletViewModel(
    private val savedStateHandle: SavedStateHandle,
    context: Context
) : ViewModel() {

    private val solanaRepo = NetworkModule.solanaRepository
    private val priceRepo = NetworkModule.priceRepository
    val nfcManager = NfcSessionManager()
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_UI_STATE, Context.MODE_PRIVATE)

    /** Шаблон транзакції до підпису; після NFC-запиту з нього збирається повна підписана tx для RPC. */
    private var pendingUnsignedTxBytes: ByteArray? = null

    private val _uiState = MutableStateFlow(loadInitialUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    val nfcState: StateFlow<NfcState> = nfcManager.state

    init {
        observeNfcResults()
    }

    /** Щоб після NDEF-інтенту / перезапуску активності залишатися на Send / Terminal, а не на home. */
    fun saveNavDestination(route: String) {
        savedStateHandle[KEY_NAV_DEST] = route
    }

    fun getInitialNavDestination(): String =
        savedStateHandle.get<String>(KEY_NAV_DEST) ?: "home"

    fun shouldShowOnboarding(): Boolean =
        !prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun completeOnboarding() {
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .apply()
    }

    fun onRefreshClicked() {
        _uiState.update {
            it.copy(showNfcSheet = true, nfcPurpose = NfcPurpose.READ_WALLET)
        }
        nfcManager.startSession(NfcMode.READ_WALLET)
    }

    fun onNfcSheetDismissed() {
        pendingUnsignedTxBytes = null
        _uiState.update { it.copy(showNfcSheet = false) }
        nfcManager.cancelSession()
    }

    fun onSendClicked() {
        val state = _uiState.value
        val recipientError = validateRecipient(state.sendRecipient)
        val amountError = validateAmount(state.sendAmount, state.balanceSol)

        if (recipientError != null || amountError != null) {
            _uiState.update { it.copy(recipientError = recipientError, amountError = amountError) }
            return
        }

        _uiState.update {
            it.copy(
                recipientError = null,
                amountError = null,
                txStatus = TxStatus.BUILDING,
                showNfcSheet = true,
                nfcPurpose = NfcPurpose.SIGN_TRANSACTION
            )
        }

        viewModelScope.launch {
            buildAndRequestSign()
        }
    }

    fun onRecipientChanged(value: String) {
        _uiState.update { it.copy(sendRecipient = value, recipientError = null) }
    }

    fun onAmountChanged(value: String) {
        _uiState.update { it.copy(sendAmount = value, amountError = null) }
    }

    fun resetTxStatus() {
        pendingUnsignedTxBytes = null
        _uiState.update {
            it.copy(
                txStatus = TxStatus.IDLE,
                txSignature = null,
                sendError = null
            )
        }
    }

    private fun observeNfcResults() {
        viewModelScope.launch {
            nfcManager.result.filterNotNull().collect { result ->
                when (result) {
                    is NfcResult.WalletRead -> {
                        val pubkey = result.payload.pubkey
                        val lastConnectedAt = System.currentTimeMillis()
                        persistLastConnectedAt(lastConnectedAt)
                        _uiState.update {
                            it.copy(
                                pubkey = pubkey,
                                lastConnectedAtMillis = lastConnectedAt,
                                showNfcSheet = false
                            )
                        }
                        fetchBalance(pubkey)
                        fetchPrice()
                    }
                    is NfcResult.SignatureRead -> {
                        _uiState.update {
                            it.copy(
                                showNfcSheet = false,
                                txStatus = TxStatus.SENDING
                            )
                        }
                        submitSignedTransaction(result.response.signature)
                    }
                    is NfcResult.Failure -> {
                        pendingUnsignedTxBytes = null
                        val s = _uiState.value
                        val txFailed = s.txStatus == TxStatus.BUILDING ||
                            s.txStatus == TxStatus.AWAITING_SIGN ||
                            s.txStatus == TxStatus.SENDING
                        _uiState.update {
                            it.copy(
                                balanceError = if (txFailed) it.balanceError else result.message,
                                sendError = if (txFailed) result.message else it.sendError,
                                txStatus = if (txFailed) TxStatus.ERROR else it.txStatus
                            )
                        }
                    }
                }
            }
        }
    }

    private fun fetchBalance(pubkey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBalance = true, balanceError = null) }
            solanaRepo.getBalance(pubkey)
                .onSuccess { balance ->
                    persistLastBalance(balance)
                    _uiState.update {
                        it.copy(balanceSol = balance, isLoadingBalance = false)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            balanceError = e.message ?: "Помилка завантаження балансу",
                            isLoadingBalance = false
                        )
                    }
                }
        }
    }

    private fun fetchPrice() {
        viewModelScope.launch {
            priceRepo.getSolPriceUsd()
                .onSuccess { price ->
                    _uiState.update { it.copy(priceUsd = price) }
                }
        }
    }

    private suspend fun buildAndRequestSign() {
        val state = _uiState.value
        val pubkey = state.pubkey ?: return

        solanaRepo.getLatestBlockhash()
            .onSuccess { blockhash ->
                val amountLamports = (state.sendAmount.toDouble() * 1_000_000_000).toLong()
                val txBytes = buildTransferTxBytes(
                    fromPubkey = pubkey,
                    toPubkey = state.sendRecipient,
                    lamports = amountLamports,
                    recentBlockhash = blockhash.blockhash
                )
                pendingUnsignedTxBytes = txBytes
                val ndefMessage = NdefProtocol.createSignRequestMessage(txBytes)
                _uiState.update { it.copy(txStatus = TxStatus.AWAITING_SIGN) }
                nfcManager.startSession(NfcMode.WRITE_SIGN_REQUEST, ndefMessage)
            }
            .onFailure { e ->
                pendingUnsignedTxBytes = null
                _uiState.update {
                    it.copy(
                        txStatus = TxStatus.ERROR,
                        sendError = "Помилка побудови транзакції: ${e.message}"
                    )
                }
            }
    }

    private suspend fun submitSignedTransaction(signatureBase64: String) {
        val template = pendingUnsignedTxBytes
        if (template == null) {
            _uiState.update {
                it.copy(
                    txStatus = TxStatus.ERROR,
                    sendError = "Внутрішня помилка: немає даних транзакції"
                )
            }
            return
        }
        val sigBytes = Base64.decode(signatureBase64, Base64.NO_WRAP)
        if (sigBytes.size != 64) {
            pendingUnsignedTxBytes = null
            _uiState.update {
                it.copy(
                    txStatus = TxStatus.ERROR,
                    sendError = "Некоректний підпис (очікувалося 64 байти)"
                )
            }
            return
        }
        val signedTx = template.copyOf()
        sigBytes.copyInto(signedTx, destinationOffset = 1)
        val signedTxBase64 = Base64.encodeToString(signedTx, Base64.NO_WRAP)

        solanaRepo.sendTransaction(signedTxBase64)
            .onSuccess { txSig ->
                pendingUnsignedTxBytes = null
                _uiState.update {
                    it.copy(
                        txSignature = txSig,
                        txStatus = TxStatus.CONFIRMING
                    )
                }
                pollConfirmation(txSig)
            }
            .onFailure { e ->
                _uiState.update {
                    it.copy(
                        txStatus = TxStatus.ERROR,
                        sendError = "Помилка надсилання: ${e.message}"
                    )
                }
            }
    }

    private suspend fun pollConfirmation(signature: String) {
        repeat(20) {
            kotlinx.coroutines.delay(2000)
            solanaRepo.getSignatureStatus(signature)
                .onSuccess { status ->
                    if (status?.confirmationStatus == "confirmed" ||
                        status?.confirmationStatus == "finalized"
                    ) {
                        _uiState.update { it.copy(txStatus = TxStatus.CONFIRMED) }
                        _uiState.value.pubkey?.let { fetchBalance(it) }
                        return
                    }
                }
        }
        _uiState.update {
            it.copy(txStatus = TxStatus.ERROR, sendError = "Таймаут підтвердження транзакції")
        }
    }

    private fun buildTransferTxBytes(
        fromPubkey: String,
        toPubkey: String,
        lamports: Long,
        recentBlockhash: String
    ): ByteArray {
        val from = Base58.decode(fromPubkey)
        val to = Base58.decode(toPubkey)
        val blockhashBytes = Base58.decode(recentBlockhash)
        val systemProgramId = ByteArray(32)

        val buf = java.io.ByteArrayOutputStream()

        // Signatures placeholder (1 signature slot, all zeros - to be filled by signer)
        buf.write(1) // num signatures required
        buf.write(ByteArray(64)) // empty signature placeholder

        // Message header
        buf.write(1) // num_required_signatures
        buf.write(0) // num_readonly_signed
        buf.write(1) // num_readonly_unsigned (system program)

        // Account keys (compact array)
        buf.write(3) // 3 accounts
        buf.write(from)
        buf.write(to)
        buf.write(systemProgramId)

        // Recent blockhash
        buf.write(blockhashBytes)

        // Instructions (compact array)
        buf.write(1) // 1 instruction

        // System Transfer instruction
        buf.write(2) // program_id_index = 2 (system program)
        buf.write(2) // num accounts
        buf.write(0) // from account index
        buf.write(1) // to account index

        // data: transfer instruction (index 2) + lamports (8 bytes LE)
        val data = ByteArray(12)
        data[0] = 2; data[1] = 0; data[2] = 0; data[3] = 0 // instruction index = 2 (transfer)
        for (i in 0..7) {
            data[4 + i] = (lamports shr (i * 8) and 0xFF).toByte()
        }
        buf.write(data.size)
        buf.write(data)

        return buf.toByteArray()
    }

    private fun validateRecipient(address: String): String? {
        if (address.isBlank()) return "Введіть адресу отримувача"
        if (!Base58.isValidSolanaAddress(address)) return "Некоректна Solana-адреса"
        if (address == _uiState.value.pubkey) return "Не можна надіслати собі"
        return null
    }

    private fun validateAmount(amount: String, balance: Double?): String? {
        if (amount.isBlank()) return "Введіть суму"
        val value = amount.toDoubleOrNull() ?: return "Некоректне число"
        if (value <= 0) return "Сума має бути більшою за 0"
        if (balance != null && value > balance - 0.000005) {
            return "Недостатньо коштів (потрібен запас на комісію)"
        }
        return null
    }

    companion object {
        private const val KEY_NAV_DEST = "nav_dest"
        private const val PREFS_UI_STATE = "solvare_ui_state"
        private const val KEY_LAST_BALANCE_SOL = "last_balance_sol"
        private const val KEY_LAST_CONNECTED_AT = "last_connected_at_ms"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }

    private fun loadInitialUiState(): WalletUiState {
        val lastBalance = prefs.getString(KEY_LAST_BALANCE_SOL, null)?.toDoubleOrNull()
        val lastConnectedAt = prefs.getLong(KEY_LAST_CONNECTED_AT, -1L)
            .takeIf { it > 0L }
        return WalletUiState(
            balanceSol = lastBalance,
            lastConnectedAtMillis = lastConnectedAt
        )
    }

    private fun persistLastBalance(balance: Double) {
        prefs.edit()
            .putString(KEY_LAST_BALANCE_SOL, balance.toString())
            .apply()
    }

    private fun persistLastConnectedAt(timestampMs: Long) {
        prefs.edit()
            .putLong(KEY_LAST_CONNECTED_AT, timestampMs)
            .apply()
    }
}
