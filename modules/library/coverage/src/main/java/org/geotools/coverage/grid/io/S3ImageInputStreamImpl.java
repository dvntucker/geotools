package org.geotools.coverage.grid.io;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;


import javax.imageio.stream.ImageInputStreamImpl;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * Created by acurtis on 9/22/16.
 */
public class S3ImageInputStreamImpl extends ImageInputStreamImpl {


    private String bucket;
    private String key;
    private AmazonS3 s3;
    private S3ObjectInputStream stream;

    private long length;
    private long position;
    private long mark_position;



    private void initStream(long offset) {
        try {
            System.out.println("Bucket: " + this.bucket + " Key:" + this.key);
            S3Object object = s3.getObject((new GetObjectRequest(this.bucket, this.key)).withRange(offset));
            ObjectMetadata meta = object.getObjectMetadata();
            this.length = meta.getContentLength();
            this.stream = object.getObjectContent();
            this.mark_position = 0;
            this.position = offset;
//            System.out.println("initStream: Connected, length = " + this.length);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

    }


    public S3ImageInputStreamImpl(URL url) {
        this.s3 = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();
        this.bucket = url.getHost();
        this.key = url.getPath();
        /* Strip leading slash */
        this.key = this.key.startsWith("/") ? this.key.substring(1) : this.key;
        this.initStream(0);
    }

    public S3ImageInputStreamImpl(String input) {
        this.s3 = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();
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
    }

    @Override
    public long getStreamPosition() {
        return this.position;
    }

    @Override
    public void seek(long pos) throws IOException {
        System.out.println("S3ImageInputStreamImpl.seek: " + pos + " Current Position: " + this.position);
        if (pos < this.position || pos > this.position + 100000) {
            this.stream.close();
            this.initStream(pos);
        } else {
            long count = pos - this.position;
            for(long i=0; i < count; i++) {
                this.read();
            }
        }
    }

    @Override
    public int skipBytes(int n) throws IOException{
        this.seek(this.position + n);
        return n;
    }

    @Override
    public long skipBytes(long n) throws IOException {
        this.seek(this.position + n);
        return n;
    }


    @Override
    public int read() throws IOException {
        position = position + 1;
        return stream.read();
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
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
        this.stream.close();
        this.initStream(this.mark_position);
        System.out.println("S3ImageInputStreamImpl.reset:  Complete");

    }

    public boolean markSupported() {
        System.out.println("S3ImageInputStreamImpl.markSupported");
        return true;
    }

    @Override
    public void close() throws IOException {
        System.out.println("S3ImageInputStreamImpl.close");
        stream.close();
    }

    public int available() throws IOException {
        System.out.println("S3ImageInputStreamImpl.available");
        return stream.available();
    }

    @Override
    public String readLine() throws IOException {
        System.out.println("S3ImageInputStreamImpl.readline");
        throw new IOException("readLine NOT Supported");
    }

}
