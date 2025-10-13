package com.safehill.kclient.models.assets

data class AssetDescriptorDiff(
    val added: List<AssetDescriptor>,
    val updated: List<AssetDescriptor>,
    val removed: List<AssetDescriptor>
) {
    companion object {
        fun generateFrom(
            oldDescriptors: Map<AssetGlobalIdentifier, AssetDescriptor>,
            newDescriptors: Map<AssetGlobalIdentifier, AssetDescriptor>
        ): AssetDescriptorDiff {
            val added = newDescriptors.keys - oldDescriptors.keys
            val removed = oldDescriptors.keys - newDescriptors.keys
            val common = newDescriptors.keys.intersect(oldDescriptors.keys)
            val updated =
                common.filter { globalId ->
                    oldDescriptors[globalId] != newDescriptors[globalId]
                }

            return AssetDescriptorDiff(
                added = added.mapNotNull { newDescriptors[it] },
                updated = updated.mapNotNull { newDescriptors[it] },
                removed = removed.mapNotNull { oldDescriptors[it] }
            )
        }
    }
}