/********************************************************************************
 *
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 ********************************************************************************/
package net.sourceforge.cruisecontrol.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;


/**
 * Class which buffers stdout from a command (through {@link StreamConsumer} interface) and
 * provides it to multiple readers as {@link InputStream} (see {@link StdoutBuffer#getContent()}).
 * The buffer can be read as many times as required.
 *
 * The implementation is thread safe.
 *
 * @author <a href="mailto:dtihelka@kky.zcu.cz">Dan Tihelka</a>
 */
final public class StdoutBuffer extends OutputStream {

  /**
   * Constructor.
   *
   * @param log the instance of logger through which to log.
   */
  public StdoutBuffer(Logger log) {
      LOG = log;
      buffer = new LinkedList<byte[]>();
      chunkSize = 1000; /* 1000 bytes in each buffer item */
      chunker = new ByteArrayOutputStream(chunkSize);
  } // StdoutBuffer

  /**
   * Implementation of {@link OutputStream#write(byte[])}
   * Adds the given data to the buffer.
   */
  @Override
  public synchronized void write(byte[] data) throws IOException {
      write(data, 0, data.length);
  } // consumeLine

  /**
   * Implementation of {@link OutputStream#write(byte[], int, int)}
   * Adds the given data to the buffer.
   */
  @Override
  public synchronized void write(byte[] b, int off, int len) throws IOException {
    /* Cannot add when closed */
    if (chunker == null) {
        throw new IOException("Tried to add data when buffer is closed");
    }

    /* Chunk the array to write and write it to the buffer */
    while (len > 0) {
        int numWrite = Math.min(chunkSize - chunker.size(), len);

        /* Write so many bytes to the stream to fill the chunker */
        chunker.write(b, off, numWrite);
        /* Move in the array */
        off += numWrite;
        len -= numWrite;

        /* Flush the chunker, if it is full */
        if (chunker.size() >= chunkSize) {
            flush();
        }
    }
  } // consumeLine

  /**
   * Implementation of {@link OutputStream#write(byte[])}
   */
  @Override
  public synchronized void write(int b) throws IOException {
    /* Cannot add when closed */
    if (chunker == null) {
        throw new IOException("Tried to add data when buffer is closed");
    }

    /* Write the byte to the stream to fill the chunker */
    chunker.write(b);
    /* Flush the chunker, if it is full */
    if (chunker.size() >= chunkSize) {
        flush();
    }
  }

  /**
   * Implementation of {@link OutputStream#close()}
   *
   * Closes the buffer, which signalizes that no more data will be written to the buffer.
   * It is necessary for {@link InputStream} returned by {@link #getContent()} to signalize
   * that all the data were read. Otherwise (the end of buffer is not known), reading from the
   * stream would block forever.
   */
  @Override
  public synchronized void close() {
    /* Already closed */
    if (chunker == null) {
        return;
    }

    /* Copy the content of chunker to the array of bytes */
    buffer.add(chunker.toByteArray());
    buffer.add(null);
    /* Release the chunker to allow freeing it - it will not be used anymore ... */
    chunker = null;
    /* Notify all threads waiting for data */
    notifyAll();
  }// close

  /**
   * Implementation of {@link OutputStream#flush()}..
   */
  @Override
  public synchronized void flush() {
    /* Cannot flush when closed or chunker is empty */
    if (chunker == null || chunker.size() == 0) {
        return;
    }

    /* Copy the content of chunker to the array of bytes */
    buffer.add(chunker.toByteArray());
    chunker.reset();
    /* Notify all threads waiting for data */
    notifyAll();
  }// flush


  /**
   * Returns stream from which the content of the buffer can be read. The method can be called multiple times (as many
   * times as wanted), always returning new reader reading buffer from the beginning.
   *
   * @return the stream to read the buffer content.
   * @throws IOException if the stream cannot be read.
   */
  public InputStream getContent() throws IOException {
    return new BufferReader(this);
  } // getContent

  /**
   * Gets the string representation of this buffer.
   * @return the string representation.
   */
  @Override
  public String toString() {
    return getClass().getName() + "[" + buffer.size() + " lines buffered with approx. " + String.valueOf(bytesNum) + "]";
  } // toString


  /*
   * ----------- ATTRIBS BLOCK -----------
   */

  /** The array with all the data passed to the buffer by {@link StreamConsumer#consumeLine(String)}. The data
   *  are stored in the buffer here, and they can read many times through stream provided by
   *  {@link #getContent()}).
   *
   *  If the last item in the buffer is <code>null</code>, it signalizes that the whole buffer
   *  was filled and no more items will be added, see {@link #close()}. */
  private final List<byte[]> buffer;
  /** The number of Bytes buffered (approximately) */
  private long         bytesNum;  // @todo bytesNum is never assigned, but is used in BufferReader.available()
  /** The size of buffer item chunk */
  private final int          chunkSize;

  /** The temporary buffer used for chunking the data. The data are first written to this buffer
   *  and when the buffer contains enough data for one chunk to be created, it is flushed to
   *  the main buffer. */
  private ByteArrayOutputStream chunker;

  /** The logger */
  private final Logger       LOG;



  /*
   * ----------- INNER CLASSES -----------
   */

  /**
   * The stream reading data from the buffer.
   */
  private final class BufferReader extends InputStream {

       /**
        * Constructor, sets to the beginning of stream.
        *
        * @param buffer the instance holding the buffered data.
        * @throws IOException if the stream cannot be read.
        */
       BufferReader(StdoutBuffer buffer) throws IOException {
          bufferInst = buffer;
          reset();
       } // BufferReader

       /** Implementation of {@link InputStream#available()} */
       @Override
       public final int available() throws IOException {
          /* Must not be closed */
          if (bufferInst == null)
              throw new IOException("Reader already closed");

          /* Return the estimated number of Bytes not read yet */
          return (int) bufferInst.bytesNum - bytesRead;
       } // available

       /** Implementation of {@link InputStream#close()} */
       @Override
       public final void close() {
           bufferInst = null;
       } // close

       /** Implementation of {@link InputStream#mark(int)}; does nothing */
       @Override
       public final void mark(int readlimit) {
           /* Mark not supported */
       } // mark

       /** Implementation of InputStream#markSupported(); always returns <code>false</code> */
       @Override
       public final boolean markSupported() {
         return false;
       } // markSupported

       /**
        *  Implementation of InputStream#read()
        *  @throws IOException if the stream cannot be read.
        */
       @Override
       public final int read() throws IOException {
          byte item[] = new byte[1];
          int res;

          /* Read one byte, return the byte if read correctly */
          if ((res = read(item)) == 1)
               return item[0];
          /* EOF reached */
          return res;
       } // read

       /**
        *  Implementation of {@link InputStream#read(byte[])}
        *  @throws IOException if the stream cannot be read.
        */
       @Override
       public final int read(byte[] outBuff) throws IOException {
          return read(outBuff, 0, outBuff.length);
       } // read

       /**
        *  Implementation of {@link InputStream#read(byte[], int, int)}
        *  @throws IOException if the stream cannot be read.
        */
       @Override
       public final int read(byte[] outBuff, int from, int len) throws IOException {
          int numRead = 0;

           /* Must not be closed */
           if (bufferInst == null)
               throw new IOException("Reader already closed");
           /* Bad state!!?? */
           if (bufferInst.buffer.size() <  bufferInd)
               throw new IOException("Reader outran the buffer?");

           /* Must be in synchronized section due to wait() method */
          synchronized (bufferInst) {

                   /* If nothing to read, wait until notified */
                   if (bufferInst.buffer.size() == bufferInd) {
                       try {
                              bufferInst.wait();
                      }
                       catch (InterruptedException tExc) {
                                LOG.error("Unexpected interruption when waiting for data", tExc);
                                return -1;
                        }
                   }

                  /* ------------
                  /* Read until data is available */
                  while (bufferInst.buffer.size() > bufferInd && numRead < len)
                  {
                           byte[] currLine = bufferInst.buffer.get(bufferInd);
                           int canRead;

                         /* If the current line is empty, EOF was reached. If at least something was read, return the number of
                          * Bytes read. Otherwise return -1 */
                         if (currLine == null)
                             return numRead > 0 ? numRead : -1;

                         /* How many items from the current buffer to read */
                         canRead = Math.min(len - numRead, currLine.length - bufferPos);
                         /* Copy the number of bytes available in the current buffer */
                         System.arraycopy(currLine, bufferPos, outBuff, from, canRead);
                         /* Shift the buffer position */
                         bufferPos += canRead;
                         numRead += canRead;
                         from += canRead;

                         /* Was the whole buffer read? Set the new if so */
                         if (bufferPos >= currLine.length) {
                             bufferInd++;
                             bufferPos  = 0;
                         }
                  }

                  /* Return what read */
                  return numRead;
          }
       } // read

       /**
        * Implementation of InputStream#reset()
        * @throws IOException when the stream is closed.
        */
       @Override
       public final void reset() throws IOException {
            /* Must not be closed */
            if (bufferInst == null)
                throw new IOException("Reader already closed");

            bufferInd = 0;
          bufferPos = 0;
          bytesRead = 0;
       } // reset

       /** Implementation of InputStream#skip() */
       @Override
       public final long skip(long num) {
           /* Skip is not provided now, reimplement by shifting in the buffer if required */
           LOG.warn(StdoutBuffer.class.getName() + ".skip() is not implemented, ignoring request");
           return 0;
       } // skip

       /**
        * Gets the string representation of this reader.
        * @return the string representation.
        */
       @Override
       public String toString() {
         return getClass().getName() + "[read " + bufferInd + " lines (" + bytesRead +
             " Bytes) from buffer with " + buffer.size() + " lines (" + bytesNum + " Bytes)]";
       } // toString

       /* ----------- ATTRIBS BLOCK ----------- */

       /** The parent instance of the buffer from which the data are read */
       private StdoutBuffer bufferInst;

       /** The index of the buffer to read */
       private int bufferInd;
       /** The index within the buffer to read */
       private int bufferPos;

       /** The number of Bytes read (approximately) */
       private int bytesRead;

  } // BufferReader

} // StdoutBuffer
