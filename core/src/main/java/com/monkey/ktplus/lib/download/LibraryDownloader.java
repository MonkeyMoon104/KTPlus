package com.monkey.ktplus.lib.download;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

public interface LibraryDownloader {
    void download(URI source, Path destination, String userAgent) throws IOException;
}
