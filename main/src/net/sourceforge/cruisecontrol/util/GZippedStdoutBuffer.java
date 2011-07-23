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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;


/**
 * Class which passes data stored StdoutBuffer through GZip compression. The StdoutBuffer,
 * thus, stores the compressed data.
 *
 * @author <a href="mailto:dtihelka@kky.zcu.cz">Dan Tihelka</a>
 */
public final class GZippedStdoutBuffer extends StdoutBuffer {
  
  /**
   * Constructor.
   * @param log the instance of logger through which to log.
   */
  public GZippedStdoutBuffer(Logger log) {
      super(log);
  } // GZippedStdoutBuffer

  /*
   * ----------- PROTECTED BLOCK -----------
   */

  /**
   * The overriding of {@link StdoutBuffer#dataEncoder(OutputStream)}
   * @return the GZipOutputStream data encoder
   */
  @Override
  protected OutputStream dataEncoder(OutputStream stream) {
    try {
        returnBaseStream = false;
        /* Create GZIP with the best compression on */
        return new GZIPOutputStream(stream) {
                       { def.setLevel(Deflater.BEST_COMPRESSION); }
                   };
    } catch (IOException e) {
        log.error("Cannot create GZipped chunker, returning the chunker from base class", e);
        returnBaseStream = true;
        return super.dataEncoder(stream);
    }
  }

  /**
   * The overriding of {@link StdoutBuffer#dataDecoder(InputStream)}
   * @return the {@link GZIPInputStream} data decoder.
   */
  @Override
  protected InputStream dataDecoder(InputStream stream) {
    /* Get the delayed input stream, otherwise GZIPInputStream constructor will block if there
     * are no data to read in stream ...  */
    return returnBaseStream ? super.dataDecoder(stream)
                            : new DelayedGZipInputStream(stream);
  }

  /*
   * ----------- ATTRIBUTES -----------
   */

  /** 
   * Set to <code>true</code>, if the allocation of {@link GZippedStdoutBuffer} in
   * {@link #dataEncoder(OutputStream)} failed and {@link StdoutBuffer#dataEncoder(OutputStream)}
   * was returned. 
   */
  private boolean returnBaseStream = false;

  /*
   * ----------- INNER CLASSES -----------
   */

  /**
   * Wrapper for GZIPInputStream. It delays the instantiation of {@link GZIPInputStream} until
   * something is read from it as {@link GZIPInputStream#GZIPInputStream(InputStream)} reads a
   * header from the stream provided. When the header is not available yet, the constructor is
   * blocked which may lead to deadlock in {@link StdoutBuffer#getContent()} call.
   * </p>
   * It passes all the calls to the GZIPInputStream class.
   */
  private class DelayedGZipInputStream extends InputStream {

    /**
     * Constructor
     * @param stream the stream passed to {@link GZIPInputStream} when instantiated 
     */
    public DelayedGZipInputStream(final InputStream stream) {
        this.stream = stream;
    }

    /**
     * Implementation of {@link InputStream#available()} 
     */
    @Override
    public final int available() throws IOException {
        return instantiate().available();
    } // available

    /**
     * Implementation of {@link InputStream#close()} 
     */
    @Override
    public final void close() throws IOException {
        instantiate().close();
    } // close

    /**
     * Implementation of {@link InputStream#mark(int)}; does nothing 
     */
    @Override
    public final void mark(int readlimit) { /* Nothing here */
    } // mark

    /**
     * Implementation of InputStream#markSupported(); always returns <code>false</code> 
     */
    @Override
    public final boolean markSupported() {
        return false;
    } // markSupported

    /**
     * Implementation of {@link InputStream#read()} 
     */
    @Override
    public final int read() throws IOException {
        return instantiate().read();
    }

    /**
     *  Implementation of {@link InputStream#read(byte[], int, int)}
     */
    @Override
    public final int read(byte[] outBuff, int from, int len) throws IOException {
        return instantiate().read(outBuff, from, len);
    } // read

    /**
     * Implementation of {@link InputStream#reset()}
     */
    @Override
    public final void reset() throws IOException {
        instantiate().reset();
    } // reset

    /**
     * Implementation of {@link InputStream#skip(long)}
     */
    @Override
    public final long skip(long num) throws IOException {
        return instantiate().skip(num);
    } // skip


    /**
     * @return If {@link #instantiated} == <code>false</code> it instantiates {@link GZIPInputStream} with
     * input set to the current {@link #stream}. If {@link #instantiated} == <code>true</code>,
     * the {@link #stream} (instance of {@link GZIPInputStream}).
     * @exception IOException if an I/O error has occurred
     */
    private InputStream instantiate() throws IOException {
        /* Instantiate, if not instantiated yet */
        if (!instantiated) {
            stream = new GZIPInputStream(stream, chunkSize);
            instantiated = true;
        }
        /* Return the current stream instance */
        return stream;
    }

    /**
     * Contains <code>true</code> if {@link #stream} is instance of {@link GZIPInputStream}, or
     * <code>false</code> if it still is the stream instance set in the constructor. 
     */
    private boolean     instantiated;
    /**
     * The input stream which the data are read from. 
     */
    private InputStream stream;

  } // DelayedGZipInputStream

} // GZippedStdoutBuffer
