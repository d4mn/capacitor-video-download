//
//  SimpleMediaSaver.swift
//
//  Created by Dennis Lysenko on 27-09-16.
//  Copyright © 2016 Dennis Lysenko. All rights reserved.
//  https://gist.github.com/dennislysenko/5388cacb83b754e8983e99bff7fef2d2
//
//  This gist is licensed under the terms of the MIT license.
//

import Foundation
import Photos

public struct PhotoLibraryError: Error {
    let reason: String
    
    var localizedDescription: String {
        return "No placeholder on Photo library change request...maybe missing permissions or disk space?\nReason: \(reason)"
    }
}

public class SimpleMediaSaver {
    public static func getPhotosCollection(withName name: String) throws -> PHAssetCollection {
        let fetchOptions = PHFetchOptions()
        fetchOptions.predicate = NSPredicate(format: "title = %@", name)
        let results = PHAssetCollection.fetchAssetCollections(with: .album, subtype: .any, options: fetchOptions)
        
        if let collection = results.firstObject {
            return collection
        } else {
            do {
                var maybePlaceholder: PHObjectPlaceholder?
                try PHPhotoLibrary.shared().performChangesAndWait {
                    let createRequest = PHAssetCollectionChangeRequest.creationRequestForAssetCollection(withTitle: name)
                    maybePlaceholder = createRequest.placeholderForCreatedAssetCollection
                }
                
                guard let placeholder = maybePlaceholder else {
                    throw PhotoLibraryError(reason: "No placeholder for collection creation request")
                }
                
                let collectionFetchRequest = PHAssetCollection.fetchAssetCollections(withLocalIdentifiers: [placeholder.localIdentifier], options: nil)
                if let collection = collectionFetchRequest.firstObject {
                    return collection
                } else {
                    throw PhotoLibraryError(reason: "No created asset")
                }
            }
        }
    }
    
    private static func saveImage(_ image: UIImage) throws -> PHObjectPlaceholder {
        var blockPlaceholder: PHObjectPlaceholder?
        
        try PHPhotoLibrary.shared().performChangesAndWait {
            let changeRequest = PHAssetChangeRequest.creationRequestForAsset(from: image)
            changeRequest.creationDate = Date()
            blockPlaceholder = changeRequest.placeholderForCreatedAsset
        }
        
        guard let placeholder = blockPlaceholder else {
            throw PhotoLibraryError(reason: "No placeholder for asset creation request")
        }
        
        return placeholder
    }
    
    private static func saveVideo(at fileURL: URL) throws -> PHObjectPlaceholder {
        var blockPlaceholder: PHObjectPlaceholder?
        
        try PHPhotoLibrary.shared().performChangesAndWait {
            let changeRequest = PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: fileURL)
            changeRequest?.creationDate = Date()
            blockPlaceholder = changeRequest?.placeholderForCreatedAsset
        }
        
        guard let placeholder = blockPlaceholder else {
            throw PhotoLibraryError(reason: "No placeholder for asset creation request, or no asset request (check if video exists at \(fileURL))")
        }
        
        return placeholder
    }

    public static func saveImage(_ image: UIImage, toCollectionWithName collectionName: String) throws -> String {
        let placeholder = try saveImage(image)
        let collection = try getPhotosCollection(withName: collectionName)
        
        try PHPhotoLibrary.shared().performChangesAndWait {
            let request = PHAssetCollectionChangeRequest(for: collection)
            request?.addAssets([placeholder] as NSArray)
        }
        
        return placeholder.localIdentifier
    }
    
    public static func saveVideo(at fileURL: URL, toCollectionWithName collectionName: String) throws -> String {
        let placeholder = try saveVideo(at: fileURL)
        let collection = try getPhotosCollection(withName: collectionName)
        
        try PHPhotoLibrary.shared().performChangesAndWait {
            let request = PHAssetCollectionChangeRequest(for: collection)
            request?.addAssets([placeholder] as NSArray)
        }
        
        return placeholder.localIdentifier
    }
}