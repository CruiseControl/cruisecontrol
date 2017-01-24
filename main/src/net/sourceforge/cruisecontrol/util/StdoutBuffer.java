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
 * Class which buffers stdout from a command (as {@link OutputStream} to which the data are written)
 * and provides it to multiple readers as {@link InputStream} (see {@link StdoutBuffer#getContent()}).
 * The buffer can be read as many times as required.
 * <p>
 * The {@link StdoutBuffer} filling and {@link StdoutBuffer#getContent()} stream reading operations are
 * thread safe. However the individual methods of {@link StdoutBuffer} and {@link StdoutBuffer#getContent()}
 * instance are not (they are supposed to be called within one thread)!
 *
 * @author <a href="mailto:dtihelka@kky.zcu.cz">Dan Tihelka</a>
 */
public class StdoutBuffer extends OutputStream {

  static final String MSG_READER_ALREADY_CLOSED = "Reader already closed";

  /**
   * Constructor.
   *
   * @param logger the instance of Logger through which to log.
   */
  public StdoutBuffer(Logger logger) {
      log = logger;
      buffer = new LinkedList<byte[]>();
      chunkSize = 1000; /* 1000 bytes in each buffer item */
      chunkBuffer = new ByteArrayOutputStream(chunkSize);
      chunkWriter = dataEncoder(chunkBuffer);
  } // StdoutBuffer

  /**
   * Implementation of {@link OutputStream#write(byte[])}
   * Adds the given data to the buffer.
   */
  @Override
  public void write(byte[] data) throws IOException {
      write(data, 0, data.length);
  } // write

  /**
   * Implementation of {@link OutputStream#write(byte[], int, int)}
   * Adds the given data to the buffer.
   */
  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    /* Cannot add when closed */
    if (chunkBuffer == null) {
        throw new IOException("Tried to add data when buffer is closed");
    }

    /* Chunk the array to write and write it to the buffer */
    while (len > 0) {
        int numWrite = Math.min(chunkSize - chunkBuffer.size(), len);

        /* Write so many bytes to the custom stream to fill the chunker */
        chunkWriter.write(b, off, numWrite);
        /* Move in the array */
        off += numWrite;
        len -= numWrite;

        /* Flush the chunker, if it is full */
        if (chunkBuffer.size() >= chunkSize) {
            flush();
        }
    }
  } // write

  /**
   * Implementation of {@link OutputStream#write(int)}
   */
  @Override
  public void write(int b) throws IOException {
    /* Cannot add when closed */
    if (chunkBuffer == null) {
        throw new IOException("Tried to add data when buffer is closed");
    }

    /* Write the byte to the custom stream to fill the chunker */
    chunkWriter.write(b);
    /* Flush the chunker, if it is full */
    if (chunkBuffer.size() >= chunkSize) {
        flush();
    }
  } // write

  /**
   * Implementation of {@link OutputStream#close()}
   * <p>
   * Closes the buffer, which signalizes that no more data will be written to the buffer.
   * It is necessary for {@link InputStream} returned by {@link #getContent()} to signalize
   * that all the data were read. Otherwise (the end of buffer is not known), reading from the
   * stream would block forever.
   */
  @Override
  public void close() {
      /* Already closed */
      if (chunkBuffer == null) {
          return;
      }
      /* Flush the custom stream to the chunker and close the custom stream */
      try {
          chunkWriter.flush();
          chunkWriter.close();
      } catch (IOException exc) {  // not likely to happen ...
          log.error("Error when closing chunk writer, the buffer will probably not be complete ...", exc);
      }

      /* Copy the content of chunker to the array of bytes */
      synchronized (buffer) {
          buffer.add(chunkBuffer.toByteArray());
          buffer.add(null);
          /* Notify all threads waiting for data */
          buffer.notifyAll();
      }
      /* Release the chunker to allow freeing it - it will not be used anymore ... */
      chunkBuffer = null;
      chunkWriter = null;
  } // close

  /**
   * Implementation of {@link OutputStream#flush()}..
   */
  @Override
  public void flush() {
      /* Cannot flush when closed or chunker is empty */
      if (chunkBuffer == null || chunkBuffer.size() == 0) {
          return;
      }
      /* Copy the content of chunker to the array of bytes */
      synchronized (buffer) {
          buffer.add(chunkBuffer.toByteArray());
          /* Notify all threads waiting for data */
          buffer.notifyAll();
      }
      chunkBuffer.reset();
  } // flush


  /**
   * Returns stream from which the content of the buffer can be read. The method can be called multiple times (as many
   * times as wanted), always returning new reader reading buffer from the beginning.
   * <p>
   * Note that reading the stream in an independent thread is save (related to writing to the buffer
   * from another thread), and it is highly recommended!
   *
   * @return the stream to read the buffer content.
   * @throws IOException if the stream cannot be read.
   */
  public InputStream getContent() throws IOException {
    return dataDecoder(new BufferReader(buffer));
  } // getContent

  /**
   * Gets the string representation of this buffer.
   * @return the string representation.
   */
  @Override
  public String toString() {
    return getClass().getName() + "[" + buffer.size() * chunkSize + " bytes in buffer (approx.)]";
  } // toString

  /*
   * ----------- PROTECTED BLOCK -----------
   */

  /**
   * Output stream customizer. The data which are required to be written to the {@link #buffer}
   * are passed through custom stream (or a sequence of streams) returned by this class. The
   * data written to the returned stream must end up in the <code>stream</code>, from which
   * they are read and stored in the {@link #buffer}:
   *
   * {@literal data -> stream[dataEncoder(OutputStream) -> {@link #buffer} ->
   *                -> stream[{@link #dataDecoder(InputStream)}]  -> data
   * }
   * <p>
   * This implementation returns back the <code>stream</code> instance.
   * <p>
   * It is ensured that this method is called prior to {@link #dataDecoder(InputStream)}.
   *
   * @param  stream the stream into which the data are required to be written once
   *         passed through the custom stream.
   * @return the custom stream through which to pass the data stored to #buffer.
   * @see    #dataDecoder(InputStream)
   */
  protected OutputStream dataEncoder(OutputStream stream) {
    return stream;
  }
  /**
   * Input stream customizer. The data which are required to be read from {@link #buffer}
   * are passed through custom stream (or a sequence of streams) returned by this class.
   * The data in form {@link #buffer} are read through <code>stream</code> stream, and
   * passed through dataDecoder(InputStream) stream. When read then, they
   * must be in the same form as written to the stream returned by {@link #dataEncoder(OutputStream)}:
   *
   * {@literal data -> stream[{@link #dataEncoder(OutputStream)}] -> {@link #buffer} ->
   *                -> stream[dataDecoder(InputStream)]  -> data
   * }
   * <p>
   * This implementation returns back the <code>stream</code>.
   *
   * @param  stream the stream into which the data are required to be written once
   *         passed through the custom stream.
   * @return the custom stream through which to pass the data stored to #buffer.
   * @see    #dataEncoder(OutputStream)
   */
  protected InputStream dataDecoder(InputStream stream) {
    return stream;
  }


  /*
   * ----------- ATTRIBS BLOCK -----------
   */

  /**
   * The array with all the data passed to the buffer through <code>write()</code> methods. The data
   * are stored in the buffer here, and they can read many times through stream provided by
   * {@link #getContent()}).
   * <p>
   * If the last item in the buffer is <code>null</code>, it signalizes that the whole buffer
   * was filled and no more items will be added, see {@link #close()}.
   * <p>
   * The work with the variable MUST BE hold in critical section. However, items are added to the buffer
   * only - once a chunk of bytes is in the buffer, it is neither changed not deleted.
   */
  private final List<byte[]> buffer;
  /**
   * The size of buffer item chunk
   */
  protected final int  chunkSize;

  /**
   * The temporary buffer used for chunking the data. The data are first written to this buffer
   * and when the buffer contains enough data for one chunk to be created, it is flushed to the
   * main buffer.
   */
  private ByteArrayOutputStream chunkBuffer;
  /**
   * The custom chunker stream returned by {@link #dataEncoder(java.io.OutputStream)}
   */
  private OutputStream   chunkWriter;

  /**
   * The logger
   */
  protected Logger log;



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
       BufferReader(List<byte[]> buffer) throws IOException {
          bufferInst = buffer;
          reset();
       } // BufferReader

       /**
        * Implementation of {@link InputStream#available()}
        */
       @Override
       public final int available() throws IOException {
          /* Must not be closed */
          if (isClosed) {
              throw new IOException(MSG_READER_ALREADY_CLOSED);
          }

          int toread;
          int last;
          /* Compute the exact number of Bytes not read yet */
          synchronized (bufferInst) {
              last = bufferInst.size() - 1;

              /* No data to read in buffer */
              if (bufferInst.size() == chunkInd) {
                  return 0;
              }
              /* EOF reached */
              if (last == chunkInd && bufferInst.get(last) == null) {
                  return -1;
              }

              /* Size of the current chunk */
              toread = bufferInst.get(chunkInd).length - chunkPos;
              /* Size of the chunks in the buffer */
              for (int ind = chunkInd + 1; ind < last; ind++) {
                   toread += bufferInst.get(ind).length;
              }
              /* The last may be null */
              if (bufferInst.get(last) != null) {
                  toread += bufferInst.get(last).length;
              }
          } // synchronized

          /* Return the result */
          return toread;
       } // available

       /**
        * Implementation of {@link InputStream#close()}
        */
       @Override
       public final void close() {
           isClosed = true;
       } // close

       /**
        * Implementation of {@link InputStream#mark(int)}; does nothing
        */
       @Override
       public final void mark(int readlimit) {
           /* Mark not supported */
       } // mark

       /**
        * Implementation of InputStream#markSupported(); always returns <code>false</code>
        */
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
           byte[] data = new byte[1];
           int numRead = read(data, 0, 1);
           int out     = data[0];

           /* Return -1 when at the end of stream */
           if (numRead < 0) {
               return -1;
           }
           /* Convert byte to 0-255 range */
           if (out < 0) {
               out = (256 + out);
           }
           /* Return -1 when at the end of stream, or the value just read instead */
           return out;
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
           byte[] currChunk;
           int numRead = 0;

           /* Must not be closed */
           if (isClosed) {
               throw new IOException(MSG_READER_ALREADY_CLOSED);
           }

           /* Read until the required number of bytes is read. */
           while (numRead < len) {

               /* Get the current buffer. */
               synchronized (bufferInst) {
                   /* Bad state!!?? */
                   if (bufferInst.size() <  chunkInd) {
                       throw new IOException("Reader outran the buffer?");
                   }

                   /* If nothing to read, wait until notified. If the required number of
                    * Bytes to read ('len' attribute) was get by available() method, it will
                    * not block */
                   if (bufferInst.size() == chunkInd) {
                       try {
                           bufferInst.wait();
                       } catch (InterruptedException tExc) {
                           log.error("Unexpected interruption when waiting for data", tExc);
                           return -1;
                       }
                   }

                   /* Get the current chunk. It cannot change once it is in the buffer */
                   currChunk = bufferInst.get(chunkInd);
               } // synchronized

               /* If the current chunk is empty, EOF was reached. If at least something was read, return the
                * number of Bytes read. Otherwise return -1 */
               if (currChunk == null) {
                  return numRead > 0 ? numRead : -1;
               }

               /* How many items from the current buffer to read */
               int canRead = Math.min(len - numRead, currChunk.length - chunkPos);
               /* Copy the number of bytes available in the current buffer */
               System.arraycopy(currChunk, chunkPos, outBuff, from, canRead);
               /* Shift the buffer position */
               chunkPos += canRead;
               numRead += canRead;
               from += canRead;

               /* Was the whole buffer read? Set the new if so */
               if (chunkPos >= currChunk.length) {
                   chunkPos  = 0;
                   chunkInd++;
               }
          }

          /* Return what read */
          return numRead;
       } // read

       /**
        * Implementation of InputStream#reset()
        * @throws IOException when the stream is closed.
        */
       @Override
       public final void reset() throws IOException {
          /* Must not be closed */
          if (isClosed) {
            throw new IOException(MSG_READER_ALREADY_CLOSED);
          }

          chunkInd = 0;
          chunkPos = 0;
       } // reset

       /**
        * Implementation of InputStream#skip()
        */
       @Override
       public final long skip(long num) {
           /* Skip is not provided now, reimplement by shifting in the buffer if required */
           log.warn(StdoutBuffer.class.getName() + ".skip() is not implemented, ignoring request");
           return 0;
       } // skip

       /**
        * Gets the string representation of this reader.
        * @return the string representation.
        */
       @Override
       public String toString() {
         return getClass().getName() + "[" + chunkInd * chunkSize + " Bytes read from buffer with "
           + buffer.size() * chunkSize + " Bytes in the buffer]";
       } // toString

       /* ----------- ATTRIBS BLOCK ----------- */

       /**
        * The parent instance of the buffer from which the data are read
        */
       private final List<byte[]> bufferInst;

       /** Flag set when {@link #close()} is called. */
       private boolean isClosed;

       /**
        * The index of the chunk to read
        */
       private int chunkInd;
       /**
        * The index within the chunk to read
        */
       private int chunkPos;

  } // BufferReader

} // StdoutBuffer
