package com.arjun.rudra.command

import android.Manifest
import android.content.Context
import com.arjun.rudra.ai.AIManager
import com.arjun.rudra.manager.*
import com.arjun.rudra.model.ParsedCommand
import com.arjun.rudra.model.RiskLevel
import com.arjun.rudra.model.Tool

/** What CommandRouter wants the UI layer to do next. */
sealed class RouterOutcome {
    data class Speak(val text: String) : RouterOutcome()
    data class NeedsConfirmation(val prompt: String, val onConfirm: suspend () -> RouterOutcome) : RouterOutcome()
    data class NeedsPermission(val permission: String, val explanation: String) : RouterOutcome()
    data class NeedsDisambiguation(val prompt: String, val candidates: List<ResolvedContact>) : RouterOutcome()
}

class CommandRouter(private val context: Context) {

    private val memory = (context.applicationContext as com.arjun.rudra.RudraApp).memory
    private val appLauncher = AppLauncher(context)
    private val contactManager = ContactManager(context, memory)
    private val callManager = CallManager(context)
    private val deviceTools = DeviceToolsManager(context)
    private val aiManager = AIManager(context)

    private val userName get() = memory.preferredUserName

    suspend fun handle(rawUtterance: String): RouterOutcome {
        val parsed = CommandParser.parse(rawUtterance)

        if (parsed.tool == Tool.UNKNOWN) {
            val reply = aiManager.ask(rawUtterance)
            return RouterOutcome.Speak(reply)
        }

        if (parsed.risk == RiskLevel.DESTRUCTIVE) {
            return RouterOutcome.NeedsConfirmation(
                prompt = confirmationPrompt(parsed),
                onConfirm = { execute(parsed) }
            )
        }

        return execute(parsed)
    }

    private suspend fun execute(cmd: ParsedCommand): RouterOutcome {
        return when (cmd.tool) {
            Tool.OPEN_APP -> {
                val ok = cmd.argument != null && appLauncher.openApp(cmd.argument)
                RouterOutcome.Speak(if (ok) "Achha, kholchi." else "$userName, ei app-ta khunje pelam na ba install nei.")
            }

            Tool.SEARCH_WEB -> {
                appLauncher.searchWeb(cmd.argument.orEmpty())
                RouterOutcome.Speak("Search korchi.")
            }

            Tool.SEARCH_YOUTUBE -> {
                appLauncher.searchYouTube(cmd.argument.orEmpty())
                RouterOutcome.Speak("YouTube e search korchi.")
            }

            Tool.CALL_CONTACT -> handleCall(cmd.argument)

            Tool.CALL_RECENT_CONTACT -> {
                if (!PermissionManager.has(context, Manifest.permission.READ_CALL_LOG)) {
                    RouterOutcome.NeedsPermission(
                        Manifest.permission.READ_CALL_LOG,
                        PermissionManager.explainMissing(Manifest.permission.READ_CALL_LOG)
                    )
                } else {
                    val number = callManager.mostRecentNumber()
                    if (number == null) {
                        RouterOutcome.Speak("$userName, recent call history khunje pelam na.")
                    } else if (!PermissionManager.has(context, Manifest.permission.CALL_PHONE)) {
                        RouterOutcome.NeedsPermission(
                            Manifest.permission.CALL_PHONE,
                            PermissionManager.explainMissing(Manifest.permission.CALL_PHONE)
                        )
                    } else {
                        callManager.call(number)
                        RouterOutcome.Speak("Achha re, call korchi.")
                    }
                }
            }

            Tool.BATTERY_STATUS -> RouterOutcome.Speak("Battery ekhon ${deviceTools.batteryPercent()} percent.")
            Tool.GET_TIME -> RouterOutcome.Speak("Ekhon shomoy ${deviceTools.currentTimeText()}.")
            Tool.GET_DATE -> RouterOutcome.Speak("Aajke ${deviceTools.currentDateText()}.")

            Tool.FLASHLIGHT_ON -> RouterOutcome.Speak(
                if (deviceTools.setFlashlight(true)) "Flashlight on kore dilam." else "Flashlight chalate parlam na."
            )
            Tool.FLASHLIGHT_OFF -> RouterOutcome.Speak(
                if (deviceTools.setFlashlight(false)) "Flashlight bondho kore dilam." else "Flashlight bondho korte parlam na."
            )

            Tool.VOLUME_UP -> { deviceTools.adjustVolume(true); RouterOutcome.Speak("Volume barhiye dilam.") }
            Tool.VOLUME_DOWN -> { deviceTools.adjustVolume(false); RouterOutcome.Speak("Volume komiye dilam.") }

            Tool.LOCK_DEVICE -> RouterOutcome.Speak(
                if (deviceTools.lockDeviceIfAdmin()) "Phone lock kore dilam."
                else "$userName, lock korte amake Device Admin permission dite hobe Settings theke."
            )

            Tool.UNKNOWN -> RouterOutcome.Speak("Bujhte parlam na, abar ekbar bolo?")
        }
    }

    private fun handleCall(nameOrLabel: String?): RouterOutcome {
        if (nameOrLabel.isNullOrBlank()) {
            return RouterOutcome.Speak("$userName, kake call korbo bolo.")
        }
        if (!PermissionManager.has(context, Manifest.permission.READ_CONTACTS)) {
            return RouterOutcome.NeedsPermission(
                Manifest.permission.READ_CONTACTS,
                PermissionManager.explainMissing(Manifest.permission.READ_CONTACTS)
            )
        }
        return when (val resolution = contactManager.resolve(nameOrLabel)) {
            is ContactResolution.NotFound ->
                RouterOutcome.Speak("\"$nameOrLabel\" naam-e kono contact pachchhi na.")

            is ContactResolution.Ambiguous -> {
                val names = resolution.candidates.joinToString(" na ") { it.displayName }
                RouterOutcome.NeedsDisambiguation("$names — konta?", resolution.candidates)
            }

            is ContactResolution.Found -> {
                if (!PermissionManager.has(context, Manifest.permission.CALL_PHONE)) {
                    return RouterOutcome.NeedsPermission(
                        Manifest.permission.CALL_PHONE,
                        PermissionManager.explainMissing(Manifest.permission.CALL_PHONE)
                    )
                }
                callManager.call(resolution.contact.phoneNumber)
                RouterOutcome.Speak("Achha re, ${resolution.contact.displayName}-ke call lagachhi.")
            }
        }
    }

    fun callResolvedContact(contact: ResolvedContact): RouterOutcome {
        if (!PermissionManager.has(context, Manifest.permission.CALL_PHONE)) {
            return RouterOutcome.NeedsPermission(
                Manifest.permission.CALL_PHONE,
                PermissionManager.explainMissing(Manifest.permission.CALL_PHONE)
            )
        }
        callManager.call(contact.phoneNumber)
        return RouterOutcome.Speak("Achha re, ${contact.displayName}-ke call lagachhi.")
    }

    private fun confirmationPrompt(cmd: ParsedCommand): String = when (cmd.tool) {
        Tool.LOCK_DEVICE -> "$userName, phone ta lock kore dibo? Confirm koro."
        else -> "$userName, eta confirm koro."
    }
}
