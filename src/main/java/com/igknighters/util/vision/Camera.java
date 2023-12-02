package com.igknighters.util.vision;

import java.util.List;
import java.util.function.Function;

import org.photonvision.PhotonCamera;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;

import com.igknighters.util.math.GeomUtil;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Transform3d;

public class Camera {
    private final PhotonCamera camera;
    private final Integer id;
    private final Transform3d cameraPose;
    private final Function<List<PhotonTrackedTarget>, List<PhotonTrackedTarget>> filter = this::filterTargets;

    public Camera(String cameraName, Integer id, Pose3d cameraPose) {
        this.camera = new PhotonCamera(cameraName);
        this.id = id;
        this.cameraPose = GeomUtil.pose3dToTransform3d(cameraPose);
    }

    private List<PhotonTrackedTarget> filterTargets(List<PhotonTrackedTarget> targets) {
        return targets.stream().filter(target -> target.getPoseAmbiguity() < 0.2).toList();
    }

    public CameraResult getLatestResult() {
        return new CameraResult(camera.getLatestResult());
    }

    public Boolean hasCalibrationData() {
        return camera.getCameraMatrix().isPresent() && camera.getDistCoeffs().isPresent();
    }

    public PhotonCamera getPhotonCamera() {
        return camera;
    }

    public Transform3d getRobotToCameraTransform3d() {
        return cameraPose;
    }

    public class CameraResult {
        private final boolean isValid;
        private final List<PhotonTrackedTarget> targets;
        private final double timestamp;

        private CameraResult(PhotonPipelineResult result) {
            boolean valid = result.hasTargets();
            if (valid) {
                valid = result.getTimestampSeconds() > 0;
            }
            this.isValid = valid;
            this.targets = filter.apply(result.getTargets());
            this.timestamp = result.getTimestampSeconds();
        }

        public Boolean isValid() {
            return isValid;
        }

        public Boolean multiTarget() {
            return targets.size() > 1;
        }

        public List<PhotonTrackedTarget> getTargets() {
            return targets;
        }

        public Transform3d getCameraTransform3d() {
            return cameraPose;
        }

        public Integer getId() {
            return id;
        }

        public double getTimestamp() {
            return timestamp;
        }

        public Camera getCamera() {
            return Camera.this;
        }
    }
}
