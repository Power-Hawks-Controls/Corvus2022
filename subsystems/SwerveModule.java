// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.trajectory.TrapezoidProfile;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel;
import com.revrobotics.SparkMaxPIDController;
import com.revrobotics.CANSparkMax.ControlType;
import com.revrobotics.SparkMaxPIDController.AccelStrategy;
import com.revrobotics.RelativeEncoder;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DutyCycle;


/**
 * This is the code to run a single swerve module <br><br>
 * It is called by the Drivetrain subsysem
 */
public class SwerveModule {
        private static final double kWheelDiameter = 4; // 0.1016 M wheel diameter (4")
        private static final double kWheelCircumference = Math.PI * kWheelDiameter;
        private static final double rpmToVelocityScaler = (kWheelCircumference / 6.12) / 60; //SDS Mk3 standard gear ratio from motor to wheel, divide by 60 to go from secs to mins

        private static final double kModuleMaxAngularVelocity = Drivetrain.kMaxAngularSpeed;
        private static final double kModuleMaxAngularAcceleration = 2 * Math.PI; // radians per second squared

        private final CANSparkMax m_driveMotor;
        private final CANSparkMax m_turningMotor;

        private final SparkMaxPIDController m_drivePID;

        private final RelativeEncoder m_driveEncoder;
        private final DigitalInput m_TurnEncoderInput;
        private final DutyCycle m_TurnPWMEncoder;
        private double turnEncoderOffset;

        // Gains are for example purposes only - must be determined for your own robot!
        private final ProfiledPIDController m_turningPIDController = new ProfiledPIDController( 1, 0, 0, new TrapezoidProfile.Constraints(kModuleMaxAngularVelocity, kModuleMaxAngularAcceleration) );

        /**
         * Constructs a SwerveModule with a drive motor, turning motor, drive encoder and turning encoder.
         *
         * @param driveMotorChannel CAN ID for the drive motor.
         * @param turningMotorChannel CAN ID for the turning motor.
         * @param driveEncoder DIO input for the drive encoder channel A
         * @param turnEncoderPWMChannel DIO input for the drive encoder channel B
         * @param turnOffset offset from 0 to 1 for the home position of the encoder
         */
        public SwerveModule(int driveMotorChannel, int turningMotorChannel, int turnEncoderPWMChannel, double turnOffset) {
            // can spark max motor controller objects
            m_driveMotor = new CANSparkMax(driveMotorChannel, CANSparkMaxLowLevel.MotorType.kBrushless);
            m_turningMotor = new CANSparkMax(turningMotorChannel, CANSparkMaxLowLevel.MotorType.kBrushless);

            m_driveMotor.setOpenLoopRampRate(0.1);

            m_drivePID = m_driveMotor.getPIDController();
            m_drivePID.setSmartMotionAccelStrategy(AccelStrategy.kTrapezoidal, 0);
            m_drivePID.setSmartMotionMaxAccel(0.2, 0);
            m_drivePID.setReference(0, CANSparkMax.ControlType.kSmartMotion);

            //spark max built-in encoder
            m_driveEncoder = m_driveMotor.getEncoder();
            m_driveEncoder.setVelocityConversionFactor(rpmToVelocityScaler);

            //PWM encoder from CTRE mag encoders
            m_TurnEncoderInput = new DigitalInput(turnEncoderPWMChannel);
            m_TurnPWMEncoder = new DutyCycle(m_TurnEncoderInput);
            turnEncoderOffset = turnOffset;

            // Limit the PID Controller's input range between -pi and pi and set the input
            // to be continuous.
            m_turningPIDController.enableContinuousInput(-Math.PI, Math.PI);
        }

        /**
         * Returns the current state of the module.
         *
         * @return The current state of the module.
         */
        public SwerveModuleState getState() {
            //the getVelocity() function normally returns RPM but is scaled in the SwerveModule constructor to return actual wheel speed
            return new SwerveModuleState( m_driveEncoder.getVelocity(), new Rotation2d(getTurnEncoderRadians()) );
        }

        /**
         * Sets the desired state for the module.
         *
         * @param desiredState Desired state with speed and angle.
         */
        public void setDesiredState(SwerveModuleState desiredState) {
            // Optimize the reference state to avoid spinning further than 90 degrees
            SwerveModuleState state = SwerveModuleState.optimize( desiredState, new Rotation2d(getTurnEncoderRadians()) );

            // Calculate the drive output from the drive PID controller.
            // final double driveOutput = m_drivePIDController.calculate( m_driveEncoder.getVelocity(), state.speedMetersPerSecond );

            

            final double signedAngleDifference = closestAngleCalculator(getTurnEncoderRadians(), state.angle.getRadians());
            double rotateMotorPercentPower = signedAngleDifference / (2* Math.PI); //proportion error control //2

            m_driveMotor.set((state.speedMetersPerSecond / Drivetrain.kMaxSpeed) * Math.cos(rotateMotorPercentPower));
            m_turningMotor.set(1.6 * rotateMotorPercentPower);
        }

        /**
         * Applies the absolute encoder offset value and converts range
         * from 0-1 to 0-2 pi radians
         * @return Angle of the absolute encoder in radians
         */
        private double getTurnEncoderRadians() {
            double appliedOffset = (m_TurnPWMEncoder.getOutput() - turnEncoderOffset) % 1;
            return appliedOffset * 2 * Math.PI;
        }

        /**
         * Calculates the closest angle and direction between two points on a circle.
         * @param currentAngle <ul><li>where you currently are</ul></li>
         * @param desiredAngle <ul><li>where you want to end up</ul></li>
         * @return <ul><li>signed double of the angle (rad) between the two points</ul></li>
         */
        public double closestAngleCalculator(double currentAngle, double desiredAngle) {
            double signedDiff = 0.0;
            double rawDiff = currentAngle > desiredAngle ? currentAngle - desiredAngle : desiredAngle - currentAngle; // find the positive raw distance between the angles
            double modDiff = rawDiff % (2 * Math.PI); // constrain the difference to a full circle

            if (modDiff > Math.PI) { // if the angle is greater than half a rotation, go backwards
                signedDiff = ((2 * Math.PI) - modDiff); //full circle minus the angle
                if (desiredAngle > currentAngle) signedDiff = signedDiff * -1; // get the direction that was lost calculating raw diff
            }
            else {
                signedDiff = modDiff;
                if (currentAngle > desiredAngle) signedDiff = signedDiff * -1;
            }
            return signedDiff;
        }
}
