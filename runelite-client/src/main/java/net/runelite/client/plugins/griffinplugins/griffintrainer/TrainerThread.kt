package net.runelite.client.plugins.griffinplugins.griffintrainer

import net.runelite.api.Skill
import net.runelite.client.plugins.griffinplugins.griffintrainer.models.PlayTimer
import net.runelite.client.plugins.griffinplugins.griffintrainer.trainers.combat.CombatTrainer
import net.runelite.client.plugins.griffinplugins.griffintrainer.trainers.mining.MiningTrainer
import net.runelite.client.plugins.microbot.Microbot
import net.runelite.client.plugins.microbot.util.Global

class TrainerThread(private val config: GriffinTrainerConfig) : Thread() {

    companion object {
        val overallTimer = PlayTimer()
        val taskTimer = PlayTimer()
        var currentTask = ""
        var countLabel = ""
        var count = 0
    }

    private enum class TrainerScripts {
        MINING, COMBAT
    }

    private lateinit var miningTrainer: MiningTrainer
    private lateinit var combatTrainer: CombatTrainer
    override fun run() {
        miningTrainer = MiningTrainer(config)
        combatTrainer = CombatTrainer(config)

        overallTimer.setRandomTimeout(config.minTotalTime(), config.maxTotalTime())
        overallTimer.start()

        var trainerSchedule = getTrainerSchedule()

        while (!overallTimer.isTimerComplete) {

            if (trainerSchedule.isEmpty()) {
                trainerSchedule = getTrainerSchedule()
            }

            if (trainerSchedule.isEmpty()) {
                break
            }

            val trainerToRun = trainerSchedule.removeAt(0)
            currentTask = trainerToRun.toString()

            taskTimer.reset()
            taskTimer.setRandomTimeout(config.minTaskTime(), config.maxTaskTime())
            taskTimer.start()

            while (!taskTimer.isTimerComplete) {
                if (runTrainer(trainerToRun)) {
                    break
                }

                Global.sleep(200, 250)
            }
        }
    }

    private fun runTrainer(trainerToRun: TrainerScripts): Boolean {
        return when (trainerToRun) {
            TrainerScripts.MINING -> miningTrainer.run()
            TrainerScripts.COMBAT -> combatTrainer.run()
            else -> false
        }
    }

    private fun getTrainerSchedule(): MutableList<TrainerScripts> {
        val schedule = mutableListOf<TrainerScripts>()

        val attackLevel = Microbot.getClientForKotlin().getRealSkillLevel(Skill.ATTACK)
        val strengthLevel = Microbot.getClientForKotlin().getRealSkillLevel(Skill.STRENGTH)
        val defenceLevel = Microbot.getClientForKotlin().getRealSkillLevel(Skill.DEFENCE)
        val miningLevel = Microbot.getClientForKotlin().getRealSkillLevel(Skill.MINING)

        if (config.trainCombat() && attackLevel < config.attackLevel() && strengthLevel < config.strengthLevel() && defenceLevel < config.defenceLevel()) {
            schedule.add(TrainerScripts.COMBAT)
        }

        if (config.trainMining() && miningLevel < config.miningLevel()) {
            schedule.add(TrainerScripts.MINING)
        }

        return schedule
    }
}