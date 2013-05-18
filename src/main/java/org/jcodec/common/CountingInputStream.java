package org.jcodec.common;

/*
* Copyright 2001-2005 The Apache Software Foundation.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*      http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import java.io.IOException;
import java.io.InputStream;

/**
* A decorating input stream that counts the number of bytes that
* have passed through so far.
*
* @author Henri Yandell
* @author Marcelo Liberato
* @version $Id: CountingInputStream.java 160202 2005-04-05 17:22:21Z roxspring $
*/
public class CountingInputStream extends ProxyInputStream {

   /** The count of bytes that have passed. */
   private int count;

   /**
    * Constructs a new CountingInputStream.
    * @param in InputStream to delegate to
    */
   public CountingInputStream(InputStream in) {
       super(in);
   }

   /**
    * Increases the count by super.read(b)'s return count
    * 
    * @see java.io.InputStream#read(byte[]) 
    */
   public int read(byte[] b) throws IOException {
       int found = super.read(b);
       this.count += (found >= 0) ? found : 0;
       return found;
   }

   /**
    * Increases the count by super.read(b, off, len)'s return count
    *
    * @see java.io.InputStream#read(byte[], int, int)
    */
   public int read(byte[] b, int off, int len) throws IOException {
       int found = super.read(b, off, len);
       this.count += (found >= 0) ? found : 0;
       return found;
   }

   /**
    * Increases the count by 1 if a byte is successfully read. 
    *
    * @see java.io.InputStream#read()
    */
   public int read() throws IOException {
       int found = super.read();
       this.count += (found >= 0) ? 1 : 0;
       return found;
   }
   
   /**
    * Increases the count by the number of skipped bytes.
    * 
    * @see java.io.InputStream#skip(long)
    */
   public long skip(final long length) throws IOException {
       final long skip = super.skip(length);
       this.count += skip;
       return skip;
   }

   /**
    * The number of bytes that have passed through this stream.
    *
    * @return the number of bytes accumulated
    */
   public int getCount() {
       return this.count;
   }

   /** 
    * Set the count back to 0. 
    *
    * @return the count previous to resetting.
    */
   public synchronized int resetCount() {
       int tmp = this.count;
       this.count = 0;
       return tmp;
   }

}