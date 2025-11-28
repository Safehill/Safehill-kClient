package com.safehill.kclient.network.api.collection

import com.safehill.kclient.models.dtos.collections.*

interface CollectionApi {

    /// Retrieve all collections accessible to the authenticated user
    /// - Returns: List of collections the user owns, has accessed, or has paid for
    suspend fun retrieveCollections(): List<CollectionOutputDTO>

    /// Get top pick collections for the authenticated user
    /// - Returns: List of public collections the user hasn't accessed yet, sorted by popularity
    suspend fun topPickCollections(): List<CollectionOutputDTO>

    /// Retrieve a single collection by its ID
    /// - Parameters:
    ///   - id: the collection identifier
    /// - Returns: The collection details
    suspend fun retrieveCollection(id: String): CollectionOutputDTO

    /// Track when a user accesses a collection
    /// - Parameters:
    ///   - id: the collection identifier
    suspend fun trackCollectionAccess(id: String)

    /// Search for collections based on criteria
    /// - Parameters:
    ///   - query: optional search query string
    ///   - searchScope: "owned" for user's owned and accessed, "all" for all discoverable collections
    ///   - visibility: optional filter by visibility
    ///   - priceRange: optional price range filter
    /// - Returns: List of collections matching the search criteria
    suspend fun searchCollections(
        query: String?,
        searchScope: String,
        visibility: CollectionVisibility?,
        priceRange: PriceRangeDTO?
    ): List<CollectionOutputDTO>

    /// Create a new collection
    /// - Parameters:
    ///   - name: the collection name
    ///   - description: the collection description
    /// - Returns: The created collection
    suspend fun createCollection(
        name: String,
        description: String
    ): CollectionOutputDTO

    /// Update an existing collection
    /// - Parameters:
    ///   - id: the collection identifier
    ///   - name: optional new name
    ///   - description: optional new description
    ///   - pricing: optional new pricing
    /// - Returns: The updated collection
    suspend fun updateCollection(
        id: String,
        name: String?,
        description: String?,
        pricing: Double?
    ): CollectionOutputDTO

    /// Archive a collection
    /// - Parameters:
    ///   - id: the collection identifier
    suspend fun archiveCollection(id: String)

    /// Soft delete a collection
    /// - Parameters:
    ///   - id: the collection identifier
    suspend fun deleteCollection(id: String)

    /// Add assets to a collection
    /// - Parameters:
    ///   - id: the collection identifier
    ///   - request: the request containing assets and decryption details
    /// - Returns: Result of the add operation
    suspend fun addAssetsToCollection(
        id: String,
        request: CollectionAssetAddRequestDTO
    ): CollectionAssetAddResultDTO

    /// Copy assets from one collection to another
    /// - Parameters:
    ///   - request: the copy request details
    /// - Returns: Result of the copy operation
    suspend fun copyAssets(
        request: CollectionAssetCopyRequestDTO
    ): CollectionAssetCopyResultDTO

    /// Change the visibility of a collection
    /// - Parameters:
    ///   - id: the collection identifier
    ///   - request: the visibility change request
    /// - Returns: Result of the visibility change operation
    suspend fun changeCollectionVisibility(
        id: String,
        request: CollectionChangeVisibilityRequestDTO
    ): CollectionChangeVisibilityResultDTO

    /// Check if the user has access to a collection
    /// - Parameters:
    ///   - collectionId: the collection identifier
    /// - Returns: Access check result with status and details
    suspend fun checkCollectionAccess(
        collectionId: String
    ): AccessCheckResultDTO

    /// Create a checkout session for purchasing access to a collection
    /// - Parameters:
    ///   - collectionId: the collection identifier
    ///   - request: the checkout session request details
    /// - Returns: Checkout session details including URL and session ID
    suspend fun createCheckoutSession(
        collectionId: String,
        request: CreateCheckoutSessionRequestDTO
    ): CheckoutSessionDTO

    /// Validate an in-app purchase receipt for a collection
    /// - Parameters:
    ///   - collectionId: the collection identifier
    ///   - request: the IAP receipt validation request
    /// - Returns: Validation response with success status
    suspend fun validateIAPReceipt(
        collectionId: String,
        request: IAPReceiptValidationRequestDTO
    ): IAPReceiptValidationResponseDTO
}
