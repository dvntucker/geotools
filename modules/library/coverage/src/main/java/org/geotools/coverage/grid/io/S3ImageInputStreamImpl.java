package org.geotools.coverage.grid.io;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import javax.imageio.stream.ImageInputStreamImpl;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;

/**
 * Created by acurtis on 9/22/16.
 */
public class S3ImageInputStreamImpl extends ImageInputStreamImpl {


    private String url;
    private String bucket;
    private String key;
    private AmazonS3 s3;

    private long length;
    private long position;
    private long mark_position = 0;

    private File cacheFile;
    private RandomAccessFile cache;
    private long cacheLength = 0;
    private static final int BLOCK_READ = 262144;
    private static final int BUFFER_LENGTH = 16384;
    private byte[] buffer = new byte[BUFFER_LENGTH];

    private static File CACHE_DIR = new File("/tmp");

    private S3ObjectInputStream initStream(long offset) {
        try {
            System.out.println("Bucket: " + this.bucket + " Key:" + this.key);
            S3Object object = s3.getObject((new GetObjectRequest(this.bucket, this.key)).withRange(offset));
            ObjectMetadata meta = object.getObjectMetadata();
            this.length = meta.getContentLength();
            System.out.println("Image Lenght: " + this.length);

            return object.getObjectContent();
//            this.position = offset;
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    private long readToCache(long pos) throws IOException {
        long nBytes = 0;
        if (pos < cacheLength) {
            // Already have this byte in the cache
            return pos;
        }
        if (cacheLength == length) {
            // Already cached the whole file
            return pos;
        }

        S3ObjectInputStream stream = this.initStream(cacheLength);

        cache.seek(cacheLength);
        long read_len = pos - cacheLength;
        System.out.println("S3ImageInputStreamImpl.readToCache: " + read_len + " more bytes needed");

        while (read_len > 0) {
            // read another block length and write it to the file cache
            int read_count = (int) (this.BLOCK_READ/this.BUFFER_LENGTH);
            for (int i=0; i < read_count; i++ ) {
                System.out.println("S3ImageInputStreamImpl.readToCache: reading bytes " + this.cacheLength + " to " + (cacheLength + BUFFER_LENGTH));
                nBytes = stream.read(buffer, 0, (int) BUFFER_LENGTH);
                System.out.println("S3ImageInputStreamImpl.readToCache: bytes read: " + nBytes);

                if (nBytes > 0) {
                    cache.write(buffer, 0, (int) nBytes);
                    read_len -= nBytes;
                    cacheLength += nBytes;
                    System.out.println("S3ImageInputStreamImpl.readToCache: bytes written: " + nBytes);
                } else {
                    break;
                }
            }
        }
        stream.close();
        return pos;


    }

    public S3ImageInputStreamImpl(URL url) throws IOException {
        this.s3 = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();
        this.bucket = url.getHost();
        this.key = url.getPath();
        /* Strip leading slash */
        this.key = this.key.startsWith("/") ? this.key.substring(1) : this.key;
        this.initStream(0);

        this.cacheFile = File.createTempFile("gt-image-", ".tmp", CACHE_DIR);
        this.cache = new RandomAccessFile(this.cacheFile, "rw");
    }

    public S3ImageInputStreamImpl(String input) throws IOException {
        this.s3 = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        this.url = input;
        String parts[] = input.split("/");
        StringBuilder keyBuilder = new StringBuilder();
        this.bucket = parts[2];
        for (int i=3; i < parts.length; i++ ) {
            keyBuilder.append("/").append(parts[i]);
        }
        this.key = keyBuilder.toString();
        /* Strip leading slash */
        this.key = this.key.startsWith("/") ? this.key.substring(1) : this.key;
        this.initStream(0);

        this.cacheFile = File.createTempFile("gt-image-", ".tmp", CACHE_DIR);
        this.cache = new RandomAccessFile(this.cacheFile, "rw");
    }

    @Override
    public long getStreamPosition() {
        System.out.println("S3ImageInputStreamImpl.position: " + this.position);
        return this.position;
    }

    @Override
    public void seek(long pos) throws IOException {
//        System.out.println("S3ImageInputStreamImpl.seek: " + pos + " Current Position: " + this.position);
        this.position = pos;
    }

    @Override
    public int skipBytes(int n) throws IOException{
        System.out.println("S3ImageInputStreamImpl.skipBytes: " + n);
        this.position += n;
        return n;
    }

    @Override
    public long skipBytes(long n) throws IOException {
        System.out.println("S3ImageInputStreamImpl.skipBytes: " + n);
        this.position += n;
        return n;
    }



    @Override
    public int read() throws IOException {
//        System.out.println("S3ImageInputStreamImpl.read: " + this.position);

        long next = this.position + 1;
        if (next > this.cacheLength) {
            this.readToCache(next);
        }
        this.cache.seek(this.position++);
        return cache.read();
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
//        System.out.println("S3ImageInputStreamImpl.read bytes - pos:" + this.position + " len:" + len);
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int c = read();
        if (c == -1) {
            return -1;
        }
        b[off] = (byte)c;

        int i = 1;
        try {
            for (; i < len ; i++) {
                c = read();
                if (c == -1) {
                    break;
                }
                b[off + i] = (byte)c;
            }
        } catch (IOException ee) {
        }
        return i;
    }

    public synchronized void mark(int readlimit) {
        System.out.println("S3ImageInputStreamImpl.mark:  " + position);
        this.mark_position = position;
    }

    @Override
    public synchronized void mark() {
        System.out.println("S3ImageInputStreamImpl.mark:  " + position);
        this.mark_position = position;
    }
    @Override
    public synchronized void reset() throws IOException {
        System.out.println("S3ImageInputStreamImpl.reset:  " + this.mark_position);
        this.position = this.mark_position;
    }

    public boolean markSupported() {
        System.out.println("S3ImageInputStreamImpl.markSupported");
        return true;
    }

    @Override
    public void close() throws IOException {
        System.out.println("S3ImageInputStreamImpl.close");
    }

    public int available() throws IOException {
        System.out.println("S3ImageInputStreamImpl.available");
        return (int)(this.length - this.position);
    }

    @Override
    public String readLine() throws IOException {
        System.out.println("S3ImageInputStreamImpl.readline");
        throw new IOException("readLine NOT Supported");
    }

}
