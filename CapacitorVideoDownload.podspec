
  Pod::Spec.new do |s|
    s.name = 'CapacitorVideoDownload'
    s.version = '0.0.1'
    s.summary = 'Enable some media features for Capacitor, such as create albums, download and save videos.'
    s.license = 'MIT'
    s.homepage = 'https://github.com/d4mn/capacitor-video-download'
    s.author = 'd4mn'
    s.source = { :git => 'https://github.com/d4mn/capacitor-video-download', :tag => s.version.to_s }
    s.source_files = 'ios/Plugin/**/*.{swift,h,m,c,cc,mm,cpp}'
    s.ios.deployment_target  = '12.0'
    s.dependency 'Capacitor'
  end
