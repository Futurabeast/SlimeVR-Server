package dev.slimevr.protocol.autobone;

import java.util.EnumMap;

import dev.slimevr.autobone.AutoBone.Epoch;
import dev.slimevr.poserecorder.PoseFrames;
import dev.slimevr.vr.processor.skeleton.SkeletonConfigValue;

public interface AutoBoneListener {

	public void onAutoBoneProcessStatus(AutoBoneProcessType processType, String message, Boolean completed, Boolean success);

	public void onAutoBoneRecordingEnd(PoseFrames recording);

	public void onAutoBoneRecordingSaveEnd();

	public void onAutoBoneEpoch(Epoch epoch);

	public void onAutoBoneEnd(EnumMap<SkeletonConfigValue, Float> configValues);
}
