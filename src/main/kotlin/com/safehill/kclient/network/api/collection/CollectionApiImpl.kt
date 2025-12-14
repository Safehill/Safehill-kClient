package com.safehill.kclient.network.api.collection

import com.safehill.kclient.models.dtos.collections.AccessCheckResultDTO
import com.safehill.kclient.models.dtos.collections.CheckoutSessionDTO
import com.safehill.kclient.models.dtos.collections.CollectionAssetAddRequestDTO
import com.safehill.kclient.models.dtos.collections.CollectionAssetAddResultDTO
import com.safehill.kclient.models.dtos.collections.CollectionAssetCopyRequestDTO
import com.safehill.kclient.models.dtos.collections.CollectionAssetCopyResultDTO
import com.safehill.kclient.models.dtos.collections.CollectionChangeVisibilityRequestDTO
import com.safehill.kclient.models.dtos.collections.CollectionChangeVisibilityResultDTO
import com.safehill.kclient.models.dtos.collections.CollectionCreateDTO
import com.safehill.kclient.models.dtos.collections.CollectionOutputDTO
import com.safehill.kclient.models.dtos.collections.CollectionSearchDTO
import com.safehill.kclient.models.dtos.collections.CollectionUpdateDTO
import com.safehill.kclient.models.dtos.collections.CollectionVisibility
import com.safehill.kclient.models.dtos.collections.CreateCheckoutSessionRequestDTO
import com.safehill.kclient.models.dtos.collections.IAPReceiptValidationRequestDTO
import com.safehill.kclient.models.dtos.collections.IAPReceiptValidationResponseDTO
import com.safehill.kclient.models.dtos.collections.PriceRangeDTO
import com.safehill.kclient.models.dtos.collections.SearchScope
import com.safehill.kclient.network.api.BaseApi
import com.safehill.kclient.network.api.RequestMethod
import com.safehill.kclient.network.api.fireRequest
import com.safehill.kclient.network.api.postRequest
import com.safehill.kclient.network.api.postRequestForResponse

class CollectionApiImpl(
    baseApi: BaseApi
) : CollectionApi, BaseApi by baseApi {

    override suspend fun retrieveCollections(): List<CollectionOutputDTO> {
        return postRequestForResponse(
            endPoint = "collections/retrieve",
            request = null
        )
    }

    override suspend fun topPickCollections(): List<CollectionOutputDTO> {
        return postRequestForResponse(
            endPoint = "collections/top-picks",
            request = null
        )
    }

    override suspend fun retrieveCollection(id: String): CollectionOutputDTO {
        return postRequestForResponse(
            endPoint = "collections/retrieve/$id",
            request = null
        )
    }

    override suspend fun trackCollectionAccess(id: String) {
        postRequest(
            endPoint = "collections/track-access/$id",
            request = null
        )
    }

    override suspend fun searchCollections(
        query: String?,
        searchScope: SearchScope,
        visibility: CollectionVisibility?,
        priceRange: PriceRangeDTO?
    ): List<CollectionOutputDTO> {
        val searchRequest = CollectionSearchDTO(
            query = query,
            searchScope = searchScope,
            visibility = visibility,
            priceRange = priceRange
        )
        return postRequestForResponse(
            endPoint = "collections/search",
            request = searchRequest
        )
    }

    override suspend fun createCollection(
        name: String,
        description: String
    ): CollectionOutputDTO {
        val createRequest = CollectionCreateDTO(
            name = name,
            description = description
        )
        return postRequestForResponse(
            endPoint = "collections",
            request = createRequest
        )
    }

    override suspend fun updateCollection(
        id: String,
        name: String?,
        description: String?,
        pricing: Double?
    ): CollectionOutputDTO {
        val updateRequest = CollectionUpdateDTO(
            name = name,
            description = description,
            pricing = pricing
        )
        return postRequestForResponse(
            endPoint = "collections/$id",
            request = updateRequest
        )
    }

    override suspend fun archiveCollection(id: String) {
        postRequest(
            endPoint = "collections/$id/archive",
            request = null
        )
    }

    override suspend fun deleteCollection(id: String) {
        fireRequest<Unit, Unit>(
            requestMethod = RequestMethod.Delete,
            endPoint = "collections/$id",
            request = null
        )
    }

    override suspend fun addAssetsToCollection(
        id: String,
        request: CollectionAssetAddRequestDTO
    ): CollectionAssetAddResultDTO {
        return postRequestForResponse(
            endPoint = "collections/$id/assets",
            request = request
        )
    }

    override suspend fun copyAssets(
        request: CollectionAssetCopyRequestDTO
    ): CollectionAssetCopyResultDTO {
        return postRequestForResponse(
            endPoint = "collections/assets/copy",
            request = request
        )
    }

    override suspend fun changeCollectionVisibility(
        id: String,
        request: CollectionChangeVisibilityRequestDTO
    ): CollectionChangeVisibilityResultDTO {
        return postRequestForResponse(
            endPoint = "collections/$id/change-visibility",
            request = request
        )
    }

    override suspend fun checkCollectionAccess(
        collectionId: String
    ): AccessCheckResultDTO {
        return fireRequest(
            requestMethod = RequestMethod.Get(emptyList()),
            endPoint = "collections/check-access/$collectionId",
            request = null
        )
    }

    override suspend fun createCheckoutSession(
        collectionId: String,
        request: CreateCheckoutSessionRequestDTO
    ): CheckoutSessionDTO {
        return postRequestForResponse(
            endPoint = "collections/checkout-session/$collectionId",
            request = request
        )
    }

    override suspend fun validateIAPReceipt(
        collectionId: String,
        request: IAPReceiptValidationRequestDTO
    ): IAPReceiptValidationResponseDTO {
        return postRequestForResponse(
            endPoint = "collections/$collectionId/validate-receipt",
            request = request
        )
    }
}
