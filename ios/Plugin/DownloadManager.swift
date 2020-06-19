//
//  DownloadManager.swift
//  youtube video downloader
//
//  Created by Maximal Mac on 18.06.2018.
//  Copyright © 2018 Vlad. All rights reserved.
//

import Foundation

protocol DownloadManagerDelegate: class {
    func didDownloadStarted(url: String)
    func didDownloadFileTo(location: URL)
    func didUpdatedProgressForFileBy(url: String, progress: Float, size: Int64, done: Int64)
}

class Download {
    var url: String
    var isDownloading = false
    var progress: Float = 0.0
    
    var downloadTask: URLSessionDownloadTask?
    var resumeData: Data?
    
    var sizeInByte: Int64 = 0
    
    init(url: String) {
        self.url = url
    }
}

class DownloadManager: NSObject {
    private var downloads: [String: Download] = [:]
    
    lazy private var session: URLSession = {
        let config = URLSessionConfiguration.background(withIdentifier: "bgSessionConfiguration")
        let session = URLSession(configuration: config, delegate: self, delegateQueue: nil)
        return session
    }()
    
    private var delegates: [String: DownloadManagerDelegate] = [String: DownloadManagerDelegate]()
    
    func addDelegate(key: String, delegate: DownloadManagerDelegate) {
        delegates[key] = delegate
    }
    
    func removeDelegate(key: String) {
        delegates[key] = nil
    }
    
    func startDownloadFileBy(_ url: String) {
        let task = Download(url: url)
        let urlRequest = URLRequest(url: URL(string: url)!)
        task.downloadTask = session.downloadTask(with: urlRequest)
        task.downloadTask!.resume()
        task.isDownloading = true
        downloads[url] = task
        if let delegate = delegates[url] {
            delegate.didDownloadStarted(url: url)
        }
    }
    
    func cancelDownloadFileBy(_ url: String) {
        guard let download = downloads[url] else { return }
        download.downloadTask?.cancel()
        downloads[url] = nil
    }
    
    func pauseDownloadFileBy(_ url: String) {
        guard let download = downloads[url] else { return }
        if download.isDownloading {
            download.downloadTask?.cancel(byProducingResumeData: { (data) in
                download.resumeData = data
            })
            download.isDownloading = false
        }
    }
    
    func resumeDownloadFileBy(_ url: String) {
        guard let download = downloads[url] else { return }
        if let resumeData = download.resumeData {
            download.downloadTask = session.downloadTask(withResumeData: resumeData)
        } else {
            let urlRequest = URLRequest(url: URL(string: url)!)
            download.downloadTask = session.downloadTask(with: urlRequest)
        }
        download.downloadTask?.resume()
        download.isDownloading = true
    }
}

extension DownloadManager: URLSessionDownloadDelegate {
    func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask, didFinishDownloadingTo location: URL) {
        guard let url = downloadTask.originalRequest?.url?.absoluteString else { return }

        guard let code = (downloadTask.response as? HTTPURLResponse)?.statusCode else { return }
        
        if let delegate = delegates[url] {
            delegate.didDownloadFileTo(location: location)
        }
    }
    
    func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask, didWriteData bytesWritten: Int64, totalBytesWritten: Int64, totalBytesExpectedToWrite: Int64) {
        
        guard let url = downloadTask.originalRequest?.url?.absoluteString,
            let download = downloads[url]  else { return }

        download.progress = round((Float(totalBytesWritten) / Float(totalBytesExpectedToWrite)) * 100)
        
        if let delegate = delegates[url] {
            delegate.didUpdatedProgressForFileBy(url: url, progress: download.progress, size: totalBytesExpectedToWrite, done: totalBytesWritten)
        }
    }
}