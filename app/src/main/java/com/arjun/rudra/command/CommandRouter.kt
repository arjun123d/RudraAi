package com.arjun.rudra.command

import android.Manifest
import android.content.Context
import com.arjun.rudra.ai.AIManager
import com.arjun.rudra.manager.*
import com.arjun.rudra.model.ParsedCommand
import com.arjun.rudra.model.RiskLevel
import com.arjun.rudra.model.Tool

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
                RouterOutcome.Speak(if (ok) "Theek hai, khol raha hoon." else "$userName, ye app nahi mila ya install nahi hai.")
            }

            Tool.SEARCH_WEB -> {
                appLauncher.searchWeb(cmd.argument.orEmpty())
                RouterOutcome.Speak("Search kar raha hoon.")
            }

            Tool.SEARCH_YOUTUBE -> {
                appLauncher.searchYouTube(cmd.argument.orEmpty())
                RouterOutcome.Speak("YouTube par search kar raha hoon.")
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
                        RouterOutcome.Speak("$userName, recent call history nahi mili.")
                    } else if (!PermissionManager.has(context, Manifest.permission.CALL_PHONE)) {
                        RouterOutcome.NeedsPermission(
                            Manifest.permission.CALL_PHONE,
                            PermissionManager.explainMissing(Manifest.permission.CALL_PHONE)
                        )
                    } else {
                        callManager.call(number)
                        RouterOutcome.Speak("Theek hai, call kar raha hoon.")
                    }
                }
            }

            Tool.BATTERY_STATUS -> RouterOutcome.Speak("Battery abhi ${deviceTools.batteryPercent()} percent hai.")
            Tool.GET_TIME -> RouterOutcome.Speak("Abhi samay ${deviceTools.currentTimeText()} hai.")
            Tool.GET_DATE -> RouterOutcome.Speak("Aaj ${deviceTools.currentDateText()} hai.")

            Tool.FLASHLIGHT_ON -> RouterOutcome.Speak(
                if (deviceTools.setFlashlight(true)) "Flashlight on kar diya." else "Flashlight chala nahi paya."
            )
            Tool.FLASHLIGHT_OFF -> RouterOutcome.Speak(
                if (deviceTools.setFlashlight(false)) "Flashlight band kar diya." else "Flashlight band nahi kar paya."
            )

            Tool.VOLUME_UP -> { deviceTools.adjustVolume(true); RouterOutcome.Speak("Volume badha diya.") }
            Tool.VOLUME_DOWN -> { deviceTools.adjustVolume(false); RouterOutcome.Speak("Volume kam kar diya.") }

            Tool.LOCK_DEVICE -> RouterOutcome.Speak(
                if (deviceTools.lockDeviceIfAdmin()) "Phone lock kar diya."
                else "$userName, lock karne ke liye mujhe Settings se Device Admin permission deni hogi."
            )

            Tool.UNKNOWN -> RouterOutcome.Speak("Samajh nahi paya, dobara bolo?")
        }
    }

    private fun handleCall(nameOrLabel: String?): RouterOutcome {
        if (nameOrLabel.isNullOrBlank()) {
            return RouterOutcome.Speak("$userName, kisko call karoon bolo.")
        }
        if (!PermissionManager.has(context, Manifest.permission.READ_CONTACTS)) {
            return RouterOutcome.NeedsPermission(
                Manifest.permission.READ_CONTACTS,
                PermissionManager.explainMissing(Manifest.permission.READ_CONTACTS)
            )
        }
        return when (val resolution = contactManager.resolve(nameOrLabel)) {
            is ContactResolution.NotFound ->
                RouterOutcome.Speak("\"$nameOrLabel\" naam ka koi contact nahi mila.")

            is ContactResolution.Ambiguous -> {
                val names = resolution.candidates.joinToString(" ya ") { it.displayName }
                RouterOutcome.NeedsDisambiguation("$names — kaunsa?", resolution.candidates)
            }

            is ContactResolution.Found -> {
                if (!PermissionManager.has(context, Manifest.permission.CALL_PHONE)) {
                    return RouterOutcome.NeedsPermission(
                        Manifest.permission.CALL_PHONE,
                        PermissionManager.explainMissing(Manifest.permission.CALL_PHONE)
                    )
                }
                callManager.call(resolution.contact.phoneNumber)
                RouterOutcome.Speak("Theek hai, ${resolution.contact.displayName} ko call laga raha hoon.")
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
        return RouterOutcome.Speak("Theek hai, ${contact.displayName} ko call laga raha hoon.")
    }

    private fun confirmationPrompt(cmd: ParsedCommand): String = when (cmd.tool) {
        Tool.LOCK_DEVICE -> "$userName, phone lock kar doon? Confirm karo."
        else -> "$userName, ise confirm karo."
    }
}
