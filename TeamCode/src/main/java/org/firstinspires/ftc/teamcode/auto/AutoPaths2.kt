package org.firstinspires.ftc.teamcode.auto

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.geometry.Vector2d
import com.acmerobotics.roadrunner.trajectory.MarkerCallback
import com.acmerobotics.roadrunner.trajectory.Trajectory
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import org.firstinspires.ftc.teamcode.drive.RRMecanumDrive
import org.firstinspires.ftc.teamcode.hardware.Bot
import java.lang.Math.toRadians
import kotlin.math.PI
import kotlin.math.roundToInt

class AutoPaths(val opMode: LinearOpMode) {

    sealed class AutoPathElement(open val name: String) {
        class Path(override val name: String, val trajectory: Trajectory): AutoPathElement(name)
        class Action(override val name: String, val runner: () -> Unit): AutoPathElement(name)
    }

    val bot: Bot = Bot.getInstance()
    val drive: RRMecanumDrive = bot.roadRunner
    val Double.toRadians get() = (toRadians(this))
    val Int.toRadians get() = (this.toDouble().toRadians)
    private fun Pose2d.reverse() = copy(heading = heading + PI)

    // (-3.36345, -0.0756263), (72-22.75, 0)
    val startPose = Pose2d(-48.0 - 24 + 9, -34.0, PI)
    private val turnedPose = Pose2d(-48.0 - 24 + 20, -36.0, PI)
    val turnTrajectory = drive.trajectoryBuilder(startPose)
            .splineToSplineHeading(turnedPose, PI)
            .splineToConstantHeading(startPose.vec(), PI)
            .build()
    private val ramIntoStack = Vector2d(-24.0, -24.0 - 12.0)
    private val avoidStack = Vector2d(-24.0, -19.0)
    private val dropFirstWobble = mapOf(
            0 to Pose2d(-0.0756263, -72.0 + 22.75 + 3.36345 - 7f, (-90.0 + 39.05465).toRadians),
            1 to Pose2d(24 - 2.668 + 2f, -24 - 11.416 - 8 - 2f, (-90 + 85.87698).toRadians),
            4 to Pose2d(48.0 + 1.4313, -48 + 5.35248 - 9f, (-90 + 31.4772).toRadians)
    )
    private val dropSecondWobble = mapOf(
            0 to Pose2d(-4.2 + 1.5, -48.0 - 3.056 + 1f, (-90.0 + 30.268).toRadians),
            1 to Pose2d(24.0 - 9.45428 + 3f, -24.0 - 25.16465, (102.4 - 90.0).toRadians),
            4 to Pose2d(48 - 5.1, -48.0 - 3.0556 - 3f, (-90.0 + 30.268).toRadians)
    )
    val startPowerShotPose = Pose2d(-4.0, -18.5 + 6.75*3 + 1, 0.0)
    val endPowerShotPose = startPowerShotPose + Pose2d(0.0, -6.75 * 3, 0.0)

    //getting second wobble goal for 1 and 4
    private val secondWobbleLocation = Vector2d(-48.0, -48.0)
    private val secondWobbleApproachAngle = 165.0.toRadians
    private val secondWobbleApproach = Pose2d(secondWobbleLocation.plus(Vector2d(18.8722, -0.622) * 0.95), secondWobbleApproachAngle)
    private val secondWobbleClawDown = Vector2d(-6.0, -24.0 * 2.0)

    val initTrajectory = listOf(
            drive.trajectoryBuilder(startPose)
                    .strafeTo(Vector2d(-48.0, -18.0))
                    .splineToSplineHeading(startPowerShotPose, 0.0)
                    .build(),
            drive.trajectoryBuilder(startPowerShotPose)
                    .strafeRight(6.75*3.0)
                    .build())

    // Kotlin 1.3 does not support inline instantiation of SAM interfaces
    class MarkerCallbackImpl(val func: () -> Unit): MarkerCallback {
        override fun onMarkerReached() = func()
    }

    private val dropWobble = AutoPathElement.Action("Drop wobble") {
        bot.wobbleClaw.lowerArmToDrop()
        bot.wobbleClaw.waitUntilTargetReached(opMode)
        bot.wobbleClaw.open()
        Thread.sleep(500)
        bot.wobbleClaw.raiseArm()
        bot.wobbleClaw.waitUntilTargetReached(opMode)
        bot.wobbleClaw.stopArm()
        bot.wobbleClaw.periodic()
    }

    private val shootRings = AutoPathElement.Action("Shoot 3 rings") {
        bot.shooter.shootRings(opMode, 3, 0.8)
        bot.shooter.turnOff()
    }

    private val pickUpWobble = AutoPathElement.Action("Pick up wobble") {
        bot.wobbleClaw.open()
        bot.wobbleClaw.lowerArm()
        bot.wobbleClaw.waitUntilTargetReached(opMode)
        bot.wobbleClaw.close()
        Thread.sleep(450)
//        bot.wobbleClaw.raiseArm()
//        bot.wobbleClaw.waitUntilTargetReached(opMode)
    }
    
    private val feedRing = AutoPathElement.Action("Feed ring") {
        bot.shooter.shootRings(opMode, 1, 0.8)
        bot.shooter.turnOff();
    }

    private fun turn(from: Double, to: Double): AutoPathElement.Action {
        return AutoPathElement.Action("Turn from ${Math.toDegrees(from).roundToInt()}deg" +
                "to ${Math.toDegrees(to).roundToInt()}deg") {
            bot.roadRunner.turn(to - from)
        }
    }

    private val trajectorySets: Map<Int, List<AutoPathElement>> = mapOf(
            0 to run {
                val secondWobble = Pose2d(secondWobbleLocation.plus(Vector2d(14.073 + 2, 11.58958 - 6)), (294.2298 + 90.0 - 12.0).toRadians)
                // intermediate waypoint for 3 point turn

                listOf(
//                        AutoPathElement.Path("Go to shoot",
//                                drive.trajectoryBuilder(startPose, 0.0)
//                                        .lineToSplineHeading(shootPose)
//                                        .build()),
//                        shootRings,
//                        turn(from=shootPose.heading, to=shootPose.heading+PI),

                        AutoPathElement.Path("Go drop first wobble",
                                drive.trajectoryBuilder(endPowerShotPose, true)
                                        .splineTo(dropFirstWobble[0]!!.vec(), dropFirstWobble[0]!!.heading)
                                        .addTemporalMarker(0.0, MarkerCallbackImpl(bot.wobbleClaw::lowerArmToDrop))
                                        //.addDisplacementMarker({ MainTeleOp.testSomething()})
                                        .build()),

                        dropWobble,

                        // intermediate point turn
                        AutoPathElement.Path("Go to second wobble",
                                drive.trajectoryBuilder(dropFirstWobble[0]!!.reverse(), dropFirstWobble[0]!!.heading + PI)
                                        .lineToSplineHeading(Pose2d(-12.0, -36.0, secondWobble.heading))
                                        .build()),

                        AutoPathElement.Action("Arm down") {
                            bot.wobbleClaw.lowerArm()
                        },

                        AutoPathElement.Path("Go to second wobble",
                                drive.trajectoryBuilder(Pose2d(-12.0, -12.0, secondWobble.heading), secondWobble.heading + PI)
                                        .lineToSplineHeading(secondWobble)
//                                        .addSpatialMarker(secondWobbleClawDown, MarkerCallbackImpl(bot.wobbleClaw::lowerArm))
                                        .build()),

                        pickUpWobble,

                        //go to deposit second wobble
                        AutoPathElement.Path("Go drop second wobble",
                                drive.trajectoryBuilder(secondWobble.reverse(), secondWobble.heading)
                                        .splineTo(dropSecondWobble[0]!!.vec(), dropSecondWobble[0]!!.heading)
                                        .build()),

                        dropWobble,

                        // go park
                        AutoPathElement.Path("Go park",
                                drive.trajectoryBuilder(dropSecondWobble[0]!!.reverse(), dropSecondWobble[0]!!.heading + PI)
                                        .splineToSplineHeading(Pose2d(12.0, -25.0, PI), 0.0)
                                        .build())
                )
            },
            1 to run {
                val secondWobbleApproach = 294.2298 + 90.0 - 8.0
                val offset = 2
                val secondWobble = Pose2d(secondWobbleLocation.plus(Vector2d(14.073 + offset * Math.cos(secondWobbleApproach.toRadians),
                        12.58958 + offset * Math.sin(secondWobbleApproach.toRadians) - 8.0)), (secondWobbleApproach).toRadians)

//                val pose2 = Pose2d(20.0, -35.0, toRadians(175.0))
//                val pose3 = Pose2d(secondWobbleLocation.x, secondWobbleLocation.y, secondWobbleApproachAngle + Math.PI)
//                val ringLoc = Vector2d(-20.0, -35.0)//looks good as per visualizer
//                val intermediate = Pose2d(-11.0, -53.0, (178.0).toRadians)
//                val beforeIntermediate = Pose2d(-6.7, -38.0, 180.0.toRadians)
//                val intermediateReturn = Pose2d(-7.68, -39.36, 90.0.toRadians)

                listOf(
//                        AutoPathElement.Path("Go to shoot",
//                                drive.trajectoryBuilder(startPose, 0.0)
//                                        .lineToSplineHeading(shootPose)
//                                        .build()),
//
//                        shootRings,
//                        turn(from=0.0, to=PI),
//                        AutoPathElement.Action("Run intake") {
//                                                             bot.intake.run()
//                        },

                        AutoPathElement.Path("Go drop first wobble",
                                drive.trajectoryBuilder(endPowerShotPose, true)
                                        .lineToSplineHeading(dropFirstWobble[1]!!.reverse())
                                        .addTemporalMarker(0.0,
                                                MarkerCallbackImpl(bot.wobbleClaw::lowerArmToDrop))
                                        .build()),

                        dropWobble,
                        AutoPathElement.Action("Raise wobble arm") {
                            bot.wobbleClaw.raiseArm()
                            bot.wobbleClaw.waitUntilTargetReached(opMode)
                            bot.wobbleClaw.stopArm()
                        },
                        AutoPathElement.Path("Make intermediate turn",
                                drive.trajectoryBuilder(dropFirstWobble[1]!!.copy(heading=dropFirstWobble[1]!!.heading + PI))
                                        .splineTo(Vector2d(8.16, -46.0), -(20.0).toRadians)
                                        .build()),

                        AutoPathElement.Action("Run intake") {
                            bot.intake.run()
                            bot.wobbleClaw.lowerArm()
                        },

                        AutoPathElement.Path("Go pick up second wobble",
                                drive.trajectoryBuilder(Pose2d(8.16, -52.8, -(20.0).toRadians), true)
                                        .splineTo(ramIntoStack, PI)
                                        .splineTo(secondWobble.vec(), secondWobble.heading + PI)
                                        .build()),

                        AutoPathElement.Action("Stop intake", bot.intake::stop),

                        pickUpWobble,

                        AutoPathElement.Path("Go drop second wobble",
                                drive.trajectoryBuilder(secondWobble, secondWobble.heading)
//                        .splineTo(Vector2d(-27.84, -45.6), -(15.0.toRadians))
                                        .lineToSplineHeading(Pose2d(0.0, -48.0, -110.0.toRadians))
                                        .splineToSplineHeading(dropSecondWobble[1]!!.reverse(), dropSecondWobble[1]!!.heading)
                                        .addTemporalMarker(0.0, MarkerCallbackImpl({ bot.shooter.warmUp(0.8) }))
                                        .build()),

                        dropWobble,
                        
                        

                        AutoPathElement.Path("Line up shooter",
                                drive.trajectoryBuilder(dropSecondWobble[1])
                                        .lineToSplineHeading(Pose2d(-1.0, -36.0, 0.0))
                                        .build()),
                        
                        feedRing,
                                
                                
                        AutoPathElement.Path("Go park",
                                drive.trajectoryBuilder(dropSecondWobble[1], true)
                                        .lineTo(Vector2d(12.0, -24.0)).build())
                        
                        
                )
            },
            4 to run {
                val intermediateTurn = Pose2d(54.0, -24.0, -(170.0.toRadians + 180.toRadians))

                listOf(
//                        AutoPathElement.Path("Go to shoot",
//                                drive.trajectoryBuilder(startPose, 0.0)
//                                        .lineToSplineHeading(shootPose)
//                                        .build()),
//
//                        shootRings,
//                        turn(from=0.0, to=PI),

                        // go to drop first wobble
                        AutoPathElement.Path("Go drop first wobble",
                                drive.trajectoryBuilder(endPowerShotPose, false)
                                        .lineToSplineHeading(dropFirstWobble[4]!!.reverse())
                                        .addTemporalMarker(0.0,
                                                MarkerCallbackImpl(bot.wobbleClaw::lowerArmToDrop))
                                        .build()),

                        dropWobble,

                        // intermediate point turn
                        AutoPathElement.Path("Make intermediate point turn",
                                drive.trajectoryBuilder(dropFirstWobble[4]!!.reverse(), dropFirstWobble[4]!!.heading + PI)
                                        .splineTo(intermediateTurn.vec(), intermediateTurn.heading)
                                        .build()),

                        AutoPathElement.Action("Lower arm") {
                            bot.wobbleClaw.lowerArm()
                        },

                        // go to get second wobble
                        AutoPathElement.Path("Go get second wobble",
                                drive.trajectoryBuilder(intermediateTurn, intermediateTurn.heading + PI)
                                        .splineTo(secondWobbleApproach.vec(), secondWobbleApproach.heading)
                                        .addSpatialMarker(secondWobbleClawDown, MarkerCallbackImpl(bot.wobbleClaw::lowerArm))
                                        .build()),

                        pickUpWobble,

//                        //go to deposit second wobble
//                        AutoPathElement.Path("Go drop second wobble",
//                                drive.trajectoryBuilder(secondWobbleApproach.reverse(), secondWobbleApproach.heading)
//                                        .splineTo(avoidStack, 0.0)
//                                        .splineTo(dropSecondWobble[4]!!.vec(), dropSecondWobble[4]!!.heading)
//                                        .build()),

                        AutoPathElement.Path("Go drop second wobble",
                        drive.trajectoryBuilder(secondWobbleApproach.reverse(), secondWobbleApproach.heading)
                                .lineToSplineHeading(dropSecondWobble[4]!!.reverse())
                                .build()),

                        dropWobble,

                        // go park
                        AutoPathElement.Path("Go park",
                                drive.trajectoryBuilder(dropSecondWobble[4]!!.reverse(), dropSecondWobble[4]!!.heading + PI)
                                        .splineTo(Vector2d(12.0, -42.24), PI)
                                        .build())
                )
            }
//            4 to listOf(
//                    // go to drop first wobble
//                    test.trajectoryBuilder(startPose, startPose.heading)
//                            .splineTo(avoidStack, 0.0)
//                            .splineTo(dropFirstWobble[4]!!.vec(), dropFirstWobble[4]!!.heading)
//                            .build(),
//                    // go get second wobble
//                    test.trajectoryBuilder(dropFirstWobble[4]!!, dropFirstWobble[4]!!.heading + 180.0.toRadians)
//                            .splineTo(secondWobbleApproach.vec(), secondWobbleApproachAngle)
//                            .build()
//            )
    )
    fun getTrajectories(a: Int): List<AutoPathElement>{
        return trajectorySets[a]!!
    }

}