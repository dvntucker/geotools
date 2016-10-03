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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;


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

    private File cacheDirectory;
    private File[] cacheFiles;
    private int cacheBlockSize;
    private int cacheBlockCount;


    private static final int MIN_BLOCK_SIZE = 262144;
    private static final int MAX_CACHE_BLOCKS = 10000;
    private byte[] buffer;

    private static File CACHE_DIR = new File("/tmp/gt-cache");


    private S3ObjectInputStream initStream(long offset) {
        try {
//            System.out.println("Bucket: " + this.bucket + " Key:" + this.key);
            S3Object object = s3.getObject((new GetObjectRequest(this.bucket, this.key)).withRange(offset));

            return object.getObjectContent();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return null;
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

        S3Object object = this.s3.getObject(new GetObjectRequest(this.bucket, this.key));
        ObjectMetadata meta = object.getObjectMetadata();
        this.length = meta.getContentLength();
        System.out.println("Image Length: " + this.length);

        String cacheDirName = this.url.replaceAll("[^A-Za-z0-9\\-]","_");
        cacheDirName = cacheDirName.replaceAll("_+","_");
        this.cacheDirectory = new File(CACHE_DIR.getAbsolutePath() + File.separator + cacheDirName);
        this.cacheBlockSize = ((this.length / this.MAX_CACHE_BLOCKS) < MIN_BLOCK_SIZE) ? MIN_BLOCK_SIZE : (int)Math.ceil((double)this.length / this.MAX_CACHE_BLOCKS);
        this.cacheBlockCount = (int)Math.ceil((double)this.length / this.cacheBlockSize);
        this.buffer = new byte[this.cacheBlockSize];
        System.out.println("Cache Info: " + this.cacheDirectory.getAbsolutePath() + " Block Size: " + this.cacheBlockSize + " Block Count: " + this.cacheBlockCount);

        this.cacheFiles = new File[this.cacheBlockCount];

        if (!this.cacheDirectory.exists()) {
            this.cacheDirectory.mkdirs();
        }

        for (int i=0; i < this.cacheBlockCount; i++) {
            this.cacheFiles[i] = new File(this.cacheDirectory.getAbsolutePath() + File.separator + String.format("%04d",i));
        }


    }

    @Override
    public long getStreamPosition() {
//        System.out.println("S3ImageInputStreamImpl.position: " + this.position);
        return this.position;
    }

    @Override
    public void seek(long pos) throws IOException {
//        System.out.println("S3ImageInputStreamImpl.seek: " + pos + " Current Position: " + this.position);
        this.position = pos;
    }

    @Override
    public int skipBytes(int n) throws IOException{
//        System.out.println("S3ImageInputStreamImpl.skipBytes: " + n);
        this.position += n;
        return n;
    }

    @Override
    public long skipBytes(long n) throws IOException {
//        System.out.println("S3ImageInputStreamImpl.skipBytes: " + n);
        this.position += n;
        return n;
    }


    synchronized private void populateCache(int block) throws IOException {
        File cacheFile = this.cacheFiles[block];
        RandomAccessFile outFile = new RandomAccessFile(cacheFile, "rw");
        int nBytes = 0;

        S3ObjectInputStream stream = this.initStream(block * this.cacheBlockSize);
        int readLength = this.cacheBlockSize;
        System.out.println("S3ImageInputStreamImpl.populateCache: Caching - " + cacheFile.getAbsolutePath());
        while (readLength > 0) {
            nBytes = stream.read(buffer, 0, readLength);
//            System.out.println("S3ImageInputStreamImpl.populateCache: bytes read: " + nBytes);

            if (nBytes > 0) {
                outFile.write(buffer, 0, nBytes);
                readLength -= nBytes;
//                System.out.println("S3ImageInputStreamImpl.populateCache: bytes written: " + nBytes);
            } else {
                break;
            }
        }

    }


    private int readFromCache(int block, long offset) {
//        System.out.println("S3ImageInputStreamImpl.readFromCache - block:" + block + " offset:" + offset);
        File cacheFile = this.cacheFiles[block];
        RandomAccessFile inFile;
        int value = 0;
        try {
            if (!cacheFile.exists()) {
                this.populateCache(block);
            }

            inFile = new RandomAccessFile(cacheFile, "r");

            inFile.seek(offset);
            value = inFile.read();
            inFile.close();
//            System.out.println("Value:" + value);
        } catch (FileNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return value;
    }

    @Override
    public int read() throws IOException {
//        System.out.println("S3ImageInputStreamImpl.read: " + this.position);
        // determine block & offset
        int block = (int)Math.floor((double)position / this.cacheBlockSize);
        long offset = position % (long)this.cacheBlockSize;
        this.position++;
        return readFromCache(block,offset);

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
//        System.out.println("S3ImageInputStreamImpl.mark:  " + position);
        this.mark_position = position;
    }

    @Override
    public synchronized void mark() {
//        System.out.println("S3ImageInputStreamImpl.mark:  " + position);
        this.mark_position = position;
    }
    @Override
    public synchronized void reset() throws IOException {
//        System.out.println("S3ImageInputStreamImpl.reset:  " + this.mark_position);
        this.position = this.mark_position;
    }

    public boolean markSupported() {
//        System.out.println("S3ImageInputStreamImpl.markSupported");
        return true;
    }

    @Override
    public void close() throws IOException {
//        System.out.println("S3ImageInputStreamImpl.close");
    }

    public int available() throws IOException {
//        System.out.println("S3ImageInputStreamImpl.available");
        return (int)(this.length - this.position);
    }

    @Override
    public String readLine() throws IOException {
//        System.out.println("S3ImageInputStreamImpl.readline");
        throw new IOException("readLine NOT Supported");
    }

}
