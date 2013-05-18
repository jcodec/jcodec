package org.jcodec.common;

/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
* 
*      http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
* A Proxy stream which acts as expected, that is it passes the method 
* calls on to the proxied stream and doesn't change which methods are 
* being called. 
* <p>
* It is an alternative base class to FilterInputStream
* to increase reusability, because FilterInputStream changes the 
* methods being called, such as read(byte[]) to read(byte[], int, int).
* 
* @author Stephen Colebourne
* @version $Id: ProxyInputStream.java 471628 2006-11-06 04:06:45Z bayard $
*/
public abstract class ProxyInputStream extends FilterInputStream {

   /**
    * Constructs a new ProxyInputStream.
    * 
    * @param proxy  the InputStream to delegate to
    */
   public ProxyInputStream(InputStream proxy) {
       super(proxy);
       // the proxy is stored in a protected superclass variable named 'in'
   }

   /** @see java.io.InputStream#read() */
   public int read() throws IOException {
       return in.read();
   }

   /** @see java.io.InputStream#read(byte[]) */
   public int read(byte[] bts) throws IOException {
       return in.read(bts);
   }

   /** @see java.io.InputStream#read(byte[], int, int) */
   public int read(byte[] bts, int st, int end) throws IOException {
       return in.read(bts, st, end);
   }

   /** @see java.io.InputStream#skip(long) */
   public long skip(long ln) throws IOException {
       return in.skip(ln);
   }

   /** @see java.io.InputStream#available() */
   public int available() throws IOException {
       return in.available();
   }

   /** @see java.io.InputStream#close() */
   public void close() throws IOException {
       in.close();
   }

   /** @see java.io.InputStream#mark(int) */
   public synchronized void mark(int idx) {
       in.mark(idx);
   }

   /** @see java.io.InputStream#reset() */
   public synchronized void reset() throws IOException {
       in.reset();
   }

   /** @see java.io.InputStream#markSupported() */
   public boolean markSupported() {
       return in.markSupported();
   }

}