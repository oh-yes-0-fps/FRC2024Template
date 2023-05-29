package com.igknighters.util.vision;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.photonvision.EstimatedRobotPose;
import org.photonvision.estimation.VisionEstimation;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.photonvision.targeting.TargetCorner;

import edu.wpi.first.apriltag.AprilTag;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.wpilibj.DriverStation;

public class VisionPoseEstimator {
    private final AprilTagFields aprilTagField;
    private final AprilTagFieldLayout aprilTagFieldLayout;
    private Camera[] cameras;

    public VisionPoseEstimator(AprilTagFields aprilTagField, Camera... cameras) {
        this.aprilTagField = aprilTagField;
        if (aprilTagField == null) {
            //my plan is to load a custom field for our testing room but for now just blow up
            throw new IllegalArgumentException("AprilTagField cannot be null");
        } else {
            try{
                aprilTagFieldLayout = AprilTagFields.k2023ChargedUp.loadAprilTagLayoutField();
            } catch (IOException e){
                throw new RuntimeException(e);
            }
        }
        if (cameras.length < 1) {
            DriverStation.reportWarning("[VisionPoseEstimator] no cameras provided", false);
        }
        this.cameras = cameras;
    }

    public String getField() {
        return aprilTagField.toString();
    }

    public List<EstimatedRobotPose> estimateCurrentPosition() {
        if (cameras.length == 0) {
            return List.of();
        }
        var results = new ArrayList<Camera.CameraResult>();
        for (var camera : cameras) {
            results.add(camera.getLatestResult());
        }
        return estimateCurrentPositionMultiCamera(results);
    }

    private List<EstimatedRobotPose> estimateCurrentPositionMultiCamera(List<Camera.CameraResult> results) {
        var estimatedPoses = new ArrayList<EstimatedRobotPose>();
        for (var result : results) {
            if (!result.isValid()) {
                continue;
            }
            var optionalPose = estimateCurrentPosition(result);
            if (optionalPose.isPresent()) {
                estimatedPoses.add(optionalPose.get());
            }
        }
        return estimatedPoses;
    }

    private Optional<EstimatedRobotPose> estimateCurrentPosition(Camera.CameraResult result) {
        if (!result.isValid()) {
            return Optional.empty();
        }
        return multiTagSolve(result);
    }

    private Optional<EstimatedRobotPose> multiTagSolve(Camera.CameraResult result) {
        if (!result.isValid() || !result.getCamera().hasCalibrationData()) {
            return Optional.empty();
        }
        var visCorners = new ArrayList<TargetCorner>();
        var knownVisTags = new ArrayList<AprilTag>();

        for (var target : result.getTargets()) {
            var optionalTagPose = aprilTagFieldLayout.getTagPose(target.getFiducialId());
            if (optionalTagPose.isEmpty()) {
                DriverStation.reportWarning("[VisionPoseEstimator] saw unknow apriltag id: " + target.getFiducialId(),
                    false);
                continue;
            }

            var tagPose = optionalTagPose.get();
            visCorners.addAll(target.getDetectedCorners());
            knownVisTags.add(new AprilTag(target.getFiducialId(), tagPose));
        }

        if (knownVisTags.size() < 2) {
            //do single tag solve
            return singleTagSolve(result);
        }

        //so ugly but only is called once
        var cameraMatrix = result.getCamera().getPhotonCamera().getCameraMatrix().get();
        var distCoeffs = result.getCamera().getPhotonCamera().getDistCoeffs().get();

        var pnpResult = VisionEstimation.estimateCamPosePNP(cameraMatrix, distCoeffs, visCorners, knownVisTags);

        var best = new Pose3d()
            .plus(pnpResult.best)
            .plus(result.getCameraTransform3d().inverse());

        return Optional.of(new EstimatedRobotPose(best, result.getTimestamp(), result.getTargets()));
    }

    private Optional<EstimatedRobotPose> singleTagSolve(Camera.CameraResult result) {
        if (!result.isValid()) {
            return Optional.empty();
        }

        PhotonTrackedTarget lowestAmbiguityTarget = null;

        double lowestAmbiguityScore = 10;

        for (PhotonTrackedTarget target : result.getTargets()) {
            double targetPoseAmbiguity = target.getPoseAmbiguity();
            // Make sure the target is a Fiducial target.
            if (targetPoseAmbiguity != -1 && targetPoseAmbiguity < lowestAmbiguityScore) {
                lowestAmbiguityScore = targetPoseAmbiguity;
                lowestAmbiguityTarget = target;
            }
        }

        if (lowestAmbiguityTarget == null) return Optional.empty();

        int targetFiducialId = lowestAmbiguityTarget.getFiducialId();

        Optional<Pose3d> targetPosition = aprilTagFieldLayout.getTagPose(targetFiducialId);

        if (targetPosition.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
                new EstimatedRobotPose(
                        targetPosition
                                .get()
                                .transformBy(lowestAmbiguityTarget.getBestCameraToTarget().inverse())
                                .transformBy(result.getCameraTransform3d().inverse()),
                        result.getTimestamp(),
                        result.getTargets()));
    }
}
