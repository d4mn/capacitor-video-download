import Foundation
import Photos
import Capacitor

public class JSDate {
    static func toString(_ date: Date) -> String {
        let formatter = ISO8601DateFormatter()
        return formatter.string(from: date)
    }
}

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitor.ionicframework.com/docs/plugins/ios
 */
@objc(CapacitorVideoDownload)
public class CapacitorVideoDownload: CAPPlugin {
    typealias JSObject = [String:Any]
    static let DEFAULT_QUANTITY = 25
    static let DEFAULT_TYPES = "photos"
    static let DEFAULT_THUMBNAIL_WIDTH = 256
    static let DEFAULT_THUMBNAIL_HEIGHT = 256
    
    // Must be lazy here because it will prompt for permissions on instantiation without it
    lazy var imageManager = PHCachingImageManager()
    
    lazy private var downloadManager = { () -> DownloadManager in
        let downloadManager = DownloadManager()
        return downloadManager
    }()
    
    private var _savedCall : CAPPluginCall?
    
    @objc func cancel(_ call: CAPPluginCall) {
        
        guard let url = call.getString("url") else { call.reject("No such download"); return }
        self.downloadManager.cancelDownloadFileBy(url)
        call.resolve()
    }
    
    @objc func saveVideo(_ call: CAPPluginCall) {
        guard let inputPath = call.getString("path") else {
            call.reject("Must provide the data path")
            return
        }
        guard call.getString("album") != nil else {
            call.reject("Must provide the album")
            return
        }
        
        _savedCall = call
        
        checkAuthorization(allowed: {
            self.downloadManager.addDelegate(key: inputPath, delegate: self)
            self.downloadManager.startDownloadFileBy(inputPath)
        }, notAllowed: {
            call.reject("Access to photos not allowed by user")
        })
        
    }
    
    func checkAuthorization(allowed: @escaping () -> Void, notAllowed: @escaping () -> Void) {
        let status = PHPhotoLibrary.authorizationStatus()
        if status == PHAuthorizationStatus.authorized {
            allowed()
        } else {
            PHPhotoLibrary.requestAuthorization({ (newStatus) in
                if newStatus == PHAuthorizationStatus.authorized {
                    allowed()
                } else {
                    notAllowed()
                }
            })
        }
    }
    
    
    func makeLocation(_ asset: PHAsset) -> JSObject {
        var loc = JSObject()
        guard let location = asset.location else {
            return loc
        }
        
        loc["latitude"] = location.coordinate.latitude
        loc["longitude"] = location.coordinate.longitude
        loc["altitude"] = location.altitude
        loc["heading"] = location.course
        loc["speed"] = location.speed
        return loc
    }
}

extension CapacitorVideoDownload: DownloadManagerDelegate {
    
    func didDownloadFileTo(location: URL) {
        print("Download finished for url \(location)")
        guard let albumId = _savedCall?.getString("album") else { return }
        do {
            // change extension because only file with video extension can be saved in photo library
            let newURL = location.deletingPathExtension().appendingPathExtension("mp4")
            try FileManager.default.moveItem(at: location, to: newURL)
            let res = try SimpleMediaSaver.saveVideo(at: newURL, toCollectionWithName: albumId)
            var ret = JSObject()
            ret["status"] = true
            ret["filePath"] = res
            self.notifyListeners("progress", data: ret)
            _savedCall?.resolve(ret)
        } catch {
            _savedCall?.reject("Failed to save video to library", nil, error)
        }
        
    }
    
    func didUpdatedProgressForFileBy(url: String, progress: Float, size: Int64, done: Int64) {
        var ret = JSObject()
        ret["size"] = size;
        ret["total"] = done
        ret["progress"] = progress
        ret["finished"] = false
        self.notifyListeners("progress", data: ret)
    }
    
    func didDownloadStarted(url: String) {
        var ret = JSObject()
        ret["started"] = true
        self.notifyListeners("status", data: ret)
    }
}
