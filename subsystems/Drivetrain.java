// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.variables.Objects;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
/** Represents a swerve drive style drivetrain. */
public class Drivetrain extends SubsystemBase {
    public static final double kMaxSpeed = 2; // 3.68 meters per second or 12.1 ft/s (max speed of SDS Mk3 with Neo motor)
    public static final double kMaxAngularSpeed = Math.PI/3; // 1/2 rotation per second

    //positions of each swerve unit on the robot
    private final Translation2d m_frontLeftLocation = new Translation2d(-0.538,  0.538);
    private final Translation2d m_frontRightLocation = new Translation2d(0.538,  0.538);
    private final Translation2d m_backLeftLocation = new Translation2d( -0.538, -0.538);
    private final Translation2d m_backRightLocation = new Translation2d( 0.538, -0.538);

    //constructor for each swerve module
    private final SwerveModule m_frontRight = new SwerveModule(21, 2, 10, 0.80625);
    private final SwerveModule m_frontLeft  = new SwerveModule(3, 4, 11, 0.4183);
    private final SwerveModule m_backLeft   = new SwerveModule(5, 6, 12, 0.06314); //0.05178
    private final SwerveModule m_backRight  = new SwerveModule(7, 8, 13, 0.62248);

    private final SwerveDriveKinematics m_kinematics = new SwerveDriveKinematics(m_frontLeftLocation, m_frontRightLocation, m_backLeftLocation, m_backRightLocation);

    private final SwerveDriveOdometry m_odometry;

    //Constructor
    public Drivetrain() {
        m_odometry = new SwerveDriveOdometry(m_kinematics, Objects.navx.getRotation2d());
    }

    /**
     * Method to drive the robot using joystick info.
     *
     * @param xSpeed Speed of the robot in the x direction (forward).
     * @param ySpeed Speed of the robot in the y direction (sideways).
     * @param rot Angular rate of the robot.
     * @param fieldRelative Whether the provided x and y speeds are relative to the field.
     */
    @SuppressWarnings("ParameterName")
    public void drive(double xSpeed, double ySpeed, double rot, boolean fieldRelative, boolean defenseHoldingMode) {
        Rotation2d robotRotation = Objects.navx.getRotation2d();
        var swerveModuleStates = m_kinematics.toSwerveModuleStates(fieldRelative ? ChassisSpeeds.fromFieldRelativeSpeeds(xSpeed, ySpeed, rot, robotRotation): new ChassisSpeeds(xSpeed, ySpeed, rot));
        SwerveDriveKinematics.desaturateWheelSpeeds(swerveModuleStates, kMaxSpeed);
        if (!defenseHoldingMode) {
            m_frontLeft.setDesiredState(swerveModuleStates[0]);
            m_frontRight.setDesiredState(swerveModuleStates[1]);
            m_backLeft.setDesiredState(swerveModuleStates[2]);
            m_backRight.setDesiredState(swerveModuleStates[3]);
        }
        else {
            m_frontLeft.setDesiredState(new SwerveModuleState(0, new Rotation2d(3 * (Math.PI / 4))));
            m_frontRight.setDesiredState(new SwerveModuleState(0, new Rotation2d((Math.PI / 4))));
            m_backLeft.setDesiredState(new SwerveModuleState(0, new Rotation2d((Math.PI / 4))));
            m_backRight.setDesiredState(new SwerveModuleState(0, new Rotation2d(3* (Math.PI / 4))));
        }

    }

    /**
     * Updates the position of the robot relative to where it started
     */
    public void updateOdometry() {
        m_odometry.update(Objects.navx.getRotation2d(), m_frontLeft.getState(), m_frontRight.getState(), m_backLeft.getState(), m_backRight.getState());
    }

    /**
     * Gives the current position and rotation of the robot (meters) based on the wheel odometry from where the robot started
     * @return Pose2d of current robot position
     */
    public Pose2d getCurrentPose2d() {
        return m_odometry.getPoseMeters();
    }

    /**
     * Converts raw module states into chassis speeds
     * @return chassis speeds object
     */
    public ChassisSpeeds getChassisSpeeds() {
        return m_kinematics.toChassisSpeeds(m_frontLeft.getState(), m_frontRight.getState(), m_backLeft.getState(), m_backRight.getState());
    }
}
