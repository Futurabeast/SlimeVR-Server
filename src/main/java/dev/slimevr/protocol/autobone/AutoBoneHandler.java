package dev.slimevr.protocol.autobone;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.tuple.Pair;

import dev.slimevr.VRServer;
import dev.slimevr.autobone.AutoBone;
import dev.slimevr.poserecorder.PoseFrames;
import dev.slimevr.poserecorder.PoseRecorder;
import dev.slimevr.vr.processor.skeleton.SkeletonConfigValue;
import io.eiren.util.StringUtils;
import io.eiren.util.collections.FastList;
import io.eiren.util.logging.LogManager;

public class AutoBoneHandler {

	private final VRServer server;
	private final PoseRecorder poseRecorder;
	private final AutoBone autoBone;

	private ReentrantLock recordingLock = new ReentrantLock();
	private Thread recordingThread = null;

	private ReentrantLock saveRecordingLock = new ReentrantLock();
	private Thread saveRecordingThread = null;

	private ReentrantLock autoBoneLock = new ReentrantLock();
	private Thread autoBoneThread = null;

	private List<AutoBoneListener> listeners = new CopyOnWriteArrayList<>();

	public AutoBoneHandler(VRServer server) {
		this.server = server;
		this.poseRecorder = new PoseRecorder(server);
		this.autoBone = new AutoBone(server);
	}

	public void addListener(AutoBoneListener listener) {
		this.listeners.add(listener);
	}

	public void removeListener(AutoBoneListener listener) {
		this.listeners.removeIf(l -> listener == l);
	}

	private void announceProcessStatus(AutoBoneProcessType processType, String message, Boolean completed, Boolean success) {
		listeners.forEach(listener -> {
			listener.onAutoBoneProcessStatus(processType, message, completed, success);
		});
	}

	private void announceProcessStatus(AutoBoneProcessType processType, String message) {
		announceProcessStatus(processType, message, null, null);
	}

	private float processFrames(PoseFrames frames) {
		return autoBone.processFrames(frames, autoBone.calcInitError, autoBone.targetHeight, (epoch) -> {
			listeners.forEach(listener -> {
				listener.onAutoBoneEpoch(epoch);
			});
		});
	}

	public void startRecording() {
		recordingLock.lock();

		try {
			// Prevent running multiple times
			if (recordingThread != null) {
				return;
			}
			
			Thread thread = new Thread(this::startRecordingThread);
			recordingThread = thread;
			thread.start();
		} finally {
			recordingLock.unlock();
		}
	}

	private void startRecordingThread() {
		try {
			if (poseRecorder.isReadyToRecord()) {
				announceProcessStatus(AutoBoneProcessType.RECORD, "Recording...");

				// 1000 samples at 20 ms per sample is 20 seconds
				int sampleCount = server.config.getInt("autobone.sampleCount", 1000);
				long sampleRate = server.config.getLong("autobone.sampleRateMs", 20L);
				Future<PoseFrames> framesFuture = poseRecorder.startFrameRecording(sampleCount, sampleRate);
				PoseFrames frames = framesFuture.get();
				LogManager.log.info("[AutoBone] Done recording!");
				
				if (server.config.getBoolean("autobone.saveRecordings", false)) {
					announceProcessStatus(AutoBoneProcessType.RECORD, "Saving recording...");
					autoBone.saveRecording(frames);
				}

				announceProcessStatus(AutoBoneProcessType.RECORD, "Done recording!", true, true);
			} else {
				announceProcessStatus(AutoBoneProcessType.RECORD, "The server is not ready to record", true, false);
				LogManager.log.severe("[AutoBone] Unable to record...");
				return;
			}
		} catch (Exception e) {
			announceProcessStatus(AutoBoneProcessType.RECORD, "Recording failed", true, false);
			LogManager.log.severe("[AutoBone] Failed recording!", e);
		} finally {
			recordingThread = null;
		}
	}

	public void saveRecording() {
		saveRecordingLock.lock();

		try {
			// Prevent running multiple times
			if (saveRecordingThread != null) {
				return;
			}
			
			Thread thread = new Thread(this::saveRecordingThread);
			saveRecordingThread = thread;
			thread.start();
		} finally {
			saveRecordingLock.unlock();
		}
	}

	private void saveRecordingThread() {
		try {
			Future<PoseFrames> framesFuture = poseRecorder.getFramesAsync();
			if(framesFuture != null) {
				announceProcessStatus(AutoBoneProcessType.SAVE, "Waiting for recording...");
				PoseFrames frames = framesFuture.get();
				
				if (frames.getTrackerCount() <= 0) {
					throw new IllegalStateException("Recording has no trackers");
				}
				
				if (frames.getMaxFrameCount() <= 0) {
					throw new IllegalStateException("Recording has no frames");
				}
				
				announceProcessStatus(AutoBoneProcessType.SAVE, "Saving recording...");
				autoBone.saveRecording(frames);
				
				announceProcessStatus(AutoBoneProcessType.SAVE, "Recording saved!", true, true);
			} else {
				announceProcessStatus(AutoBoneProcessType.SAVE, "No recording found", true, false);
				LogManager.log.severe("[AutoBone] Unable to save, no recording was done...");
				return;
			}
		} catch (Exception e) {
			announceProcessStatus(AutoBoneProcessType.SAVE, "Failed to save recording", true, false);
			LogManager.log.severe("[AutoBone] Failed to save recording!", e);
		} finally {
			saveRecordingThread = null;
		}
	}

	public void processRecording() {
		autoBoneLock.lock();

		try {
			// Prevent running multiple times
			if (autoBoneThread != null) {
				return;
			}

			Thread thread = new Thread(this::processRecordingThread);
			autoBoneThread = thread;
			thread.start();
		} finally {
			autoBoneLock.unlock();
		}
	}

	private void processRecordingThread() {
		try {
			announceProcessStatus(AutoBoneProcessType.PROCESS, "Loading recordings...");
			List<Pair<String, PoseFrames>> frameRecordings = autoBone.loadRecordings();
			
			if (!frameRecordings.isEmpty()) {
				LogManager.log.info("[AutoBone] Done loading frames!");
			} else {
				Future<PoseFrames> framesFuture = poseRecorder.getFramesAsync();
				if(framesFuture != null) {
					announceProcessStatus(AutoBoneProcessType.PROCESS, "Waiting for recording...");
					PoseFrames frames = framesFuture.get();
					
					if (frames.getTrackerCount() <= 0) {
						throw new IllegalStateException("Recording has no trackers");
					}
					
					if (frames.getMaxFrameCount() <= 0) {
						throw new IllegalStateException("Recording has no frames");
					}
					
					frameRecordings.add(Pair.of("<Recording>", frames));
				} else {
					announceProcessStatus(AutoBoneProcessType.PROCESS, "No recordings found...", true, false);
					LogManager.log.severe("[AutoBone] No recordings found in \"" + AutoBone.getLoadDir().getPath() + "\" and no recording was done...");
					return;
				}
			}
			
			announceProcessStatus(AutoBoneProcessType.PROCESS, "Processing recording(s)...");
			LogManager.log.info("[AutoBone] Processing frames...");
			FastList<Float> heightPercentError = new FastList<Float>(frameRecordings.size());
			for (Pair<String, PoseFrames> recording : frameRecordings) {
				LogManager.log.info("[AutoBone] Processing frames from \"" + recording.getKey() + "\"...");
				
				heightPercentError.add(processFrames(recording.getValue()));
				LogManager.log.info("[AutoBone] Done processing!");
				
				//#region Stats/Values
				Float neckLength = autoBone.getConfig(SkeletonConfigValue.NECK);
				Float chestDistance = autoBone.getConfig(SkeletonConfigValue.CHEST);
				Float torsoLength = autoBone.getConfig(SkeletonConfigValue.TORSO);
				Float hipWidth = autoBone.getConfig(SkeletonConfigValue.HIPS_WIDTH);
				Float legsLength = autoBone.getConfig(SkeletonConfigValue.LEGS_LENGTH);
				Float kneeHeight = autoBone.getConfig(SkeletonConfigValue.KNEE_HEIGHT);
				
				float neckTorso = neckLength != null && torsoLength != null ? neckLength / torsoLength : 0f;
				float chestTorso = chestDistance != null && torsoLength != null ? chestDistance / torsoLength : 0f;
				float torsoWaist = hipWidth != null && torsoLength != null ? hipWidth / torsoLength : 0f;
				float legTorso = legsLength != null && torsoLength != null ? legsLength / torsoLength : 0f;
				float legBody = legsLength != null && torsoLength != null && neckLength != null ? legsLength / (torsoLength + neckLength) : 0f;
				float kneeLeg = kneeHeight != null && legsLength != null ? kneeHeight / legsLength : 0f;
				
				LogManager.log.info("[AutoBone] Ratios: [{Neck-Torso: " + StringUtils.prettyNumber(neckTorso) + "}, {Chest-Torso: " + StringUtils.prettyNumber(chestTorso) + "}, {Torso-Waist: " + StringUtils.prettyNumber(torsoWaist) + "}, {Leg-Torso: " + StringUtils.prettyNumber(legTorso) + "}, {Leg-Body: " + StringUtils.prettyNumber(legBody) + "}, {Knee-Leg: " + StringUtils.prettyNumber(kneeLeg) + "}]");
				LogManager.log.info("[AutoBone] Length values: " + autoBone.getLengthsString());
			}
			
			if (!heightPercentError.isEmpty()) {
				float mean = 0f;
				for (float val : heightPercentError) {
					mean += val;
				}
				mean /= heightPercentError.size();
				
				float std = 0f;
				for (float val : heightPercentError) {
					float stdVal = val - mean;
					std += stdVal * stdVal;
				}
				std = (float) Math.sqrt(std / heightPercentError.size());
				
				LogManager.log.info("[AutoBone] Average height error: " + StringUtils.prettyNumber(mean, 6) + " (SD " + StringUtils.prettyNumber(std, 6) + ")");
			}
			//#endregion

			announceProcessStatus(AutoBoneProcessType.PROCESS, "Done processing!", true, true);
			listeners.forEach(listener -> {
				listener.onAutoBoneEnd(autoBone.configs);
			});
		} catch (Exception e) {
			announceProcessStatus(AutoBoneProcessType.PROCESS, "Processing failed", true, false);
			LogManager.log.severe("[AutoBone] Failed adjustment!", e);
		} finally {
			autoBoneThread = null;
		}
	}

	public void applyValues() {
		autoBone.applyConfig();
		// TODO Update GUI values after applying? Is that needed here?
	}
}
