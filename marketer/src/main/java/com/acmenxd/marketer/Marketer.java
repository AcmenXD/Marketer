package com.acmenxd.marketer;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;

/**
 * @author AcmenXD
 * @version v1.0
 * @github https://github.com/AcmenXD
 * @date 2017/6/26 11:19
 * @detail 获取渠道信息
 */
public final class Marketer {
	private static final String EMPTY_STRING = "";
	private static String sCachedMarket;

	public static String getMarket(final Object context) {
		return getMarket(context, EMPTY_STRING);
	}

	public static synchronized String getMarket(final Object context, final String defaultValue) {
		if (sCachedMarket == null) {
			sCachedMarket = getMarketInternal(context, defaultValue).market;
		}
		return sCachedMarket;
	}

	public static MarketInfo getMarketInfo(final Object context) {
		return getMarketInfo(context, EMPTY_STRING);
	}

	public static synchronized MarketInfo getMarketInfo(final Object context, final String defaultValue) {
		return getMarketInternal(context, defaultValue);
	}

	private static MarketInfo getMarketInternal(final Object context, final String defaultValue) {
		String market;
		Exception error;
		try {
			final String sourceDir = Helper.getSourceDir(context);
			market = Helper.readMarket(new File(sourceDir));
			error = null;
		} catch (Exception e) {
			market = null;
			error = e;
		}
		return new MarketInfo(market == null ? defaultValue : market, error);
	}

	public static final class MarketInfo {
		public final String market;
		public final Exception error;

		public MarketInfo(final String market, final Exception error) {
			this.market = market;
			this.error = error;
		}

		@Override
		public String toString() {
			return "MarketInfo{market='" + this.market + '\'' + ", error=" + this.error + '}';
		}
	}

	public static class MarketExistsException extends IOException {
		public MarketExistsException() {
			super();
		}

		public MarketExistsException(final String message) {
			super(message);
		}
	}

	public static class MarketNotFoundException extends IOException {
		public MarketNotFoundException() {
			super();
		}

		public MarketNotFoundException(final String message) {
			super(message);
		}
	}

	public static class Helper {
		static final String UTF_8 = "UTF-8";
		static final int ZIP_COMMENT_MAX_LENGTH = 65535;
		static final int SHORT_LENGTH = 2;
		static final byte[] MAGIC = new byte[] { 0x21, 0x5a, 0x58, 0x4b, 0x21 }; // !ZXK!

		// for android code
		private static String getSourceDir(final Object context) throws ClassNotFoundException,
				InvocationTargetException, IllegalAccessException, NoSuchFieldException, NoSuchMethodException {
			final Class<?> contextClass = Class.forName("android.content.Context");
			final Class<?> applicationInfoClass = Class.forName("android.content.pm.ApplicationInfo");
			final Method getApplicationInfoMethod = contextClass.getMethod("getApplicationInfo");
			final Object appInfo = getApplicationInfoMethod.invoke(context);
			// try ApplicationInfo.publicSourceDir
			final Field publicSourceDirField = applicationInfoClass.getField("publicSourceDir");
			String sourceDir = (String) publicSourceDirField.get(appInfo);
			if (sourceDir == null) {
				// try ApplicationInfo.sourceDir
				final Field sourceDirField = applicationInfoClass.getField("sourceDir");
				sourceDir = (String) sourceDirField.get(appInfo);
			}
			if (sourceDir == null) {
				// try Context.getPackageCodePath()
				final Method getPackageCodePathMethod = contextClass.getMethod("getPackageCodePath");
				sourceDir = (String) getPackageCodePathMethod.invoke(context);
			}
			return sourceDir;

		}

		private static boolean isMagicMatched(byte[] buffer) {
			if (buffer.length != MAGIC.length) {
				return false;
			}
			for (int i = 0; i < MAGIC.length; ++i) {
				if (buffer[i] != MAGIC[i]) {
					return false;
				}
			}
			return true;
		}

		private static void writeBytes(byte[] data, DataOutput out) throws IOException {
			out.write(data);
		}

		private static void writeShort(int i, DataOutput out) throws IOException {
			ByteBuffer bb = ByteBuffer.allocate(SHORT_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
			bb.putShort((short) i);
			out.write(bb.array());
		}

		private static short readShort(DataInput input) throws IOException {
			byte[] buf = new byte[SHORT_LENGTH];
			input.readFully(buf);
			ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
			return bb.getShort(0);
		}

		public static void writeZipComment(File file, String comment) throws IOException {
			if (hasZipCommentMagic(file)) {
				throw new MarketExistsException("Zip comment already exists, ignore.");
			}
			// {@see java.util.zip.ZipOutputStream.writeEND}
			byte[] data = comment.getBytes(UTF_8);
			String cString = new String(data);
			final RandomAccessFile raf = new RandomAccessFile(file, "rw");
			raf.seek(file.length() - SHORT_LENGTH);
			// write zip comment length
			// (content field length + length field length + magic field length)
			writeShort(data.length + SHORT_LENGTH + MAGIC.length, raf);
			// write content
			writeBytes(data, raf);
			// write content length
			writeShort(data.length, raf);
			// write magic bytes
			writeBytes(MAGIC, raf);
			raf.close();
		}

		public static boolean hasZipCommentMagic(File file) throws IOException {
			RandomAccessFile raf = null;
			try {
				raf = new RandomAccessFile(file, "r");
				long index = raf.length();
				byte[] buffer = new byte[MAGIC.length];
				index -= MAGIC.length;
				// read magic bytes
				raf.seek(index);
				raf.readFully(buffer);
				// check magic bytes matched
				return isMagicMatched(buffer);
			} finally {
				if (raf != null) {
					raf.close();
				}
			}
		}

		public static String readZipComment(File file) throws IOException {
			RandomAccessFile raf = null;
			try {
				raf = new RandomAccessFile(file, "r");
				long index = raf.length();
				byte[] buffer = new byte[MAGIC.length];
				index -= MAGIC.length;
				// read magic bytes
				raf.seek(index);
				raf.readFully(buffer);
				// if magic bytes matched
				if (isMagicMatched(buffer)) {
					index -= SHORT_LENGTH;
					raf.seek(index);
					// read content length field
					int length = readShort(raf);
					if (length > 0) {
						index -= length;
						raf.seek(index);
						// read content bytes
						byte[] bytesComment = new byte[length];
						raf.readFully(bytesComment);
						return new String(bytesComment, UTF_8);
					} else {
						throw new MarketNotFoundException("Zip comment content not found");
					}
				} else {
					throw new MarketNotFoundException("Zip comment magic bytes not found");
				}
			} finally {
				if (raf != null) {
					raf.close();
				}
			}
		}

		private static String readZipCommentMmp(File file) throws IOException {
			final int mappedSize = 10240;
			final long fz = file.length();
			RandomAccessFile raf = null;
			MappedByteBuffer map = null;
			try {
				raf = new RandomAccessFile(file, "r");
				map = raf.getChannel().map(MapMode.READ_ONLY, fz - mappedSize, mappedSize);
				map.order(ByteOrder.LITTLE_ENDIAN);
				int index = mappedSize;
				byte[] buffer = new byte[MAGIC.length];
				index -= MAGIC.length;
				// read magic bytes
				map.position(index);
				map.get(buffer);
				// if magic bytes matched
				if (isMagicMatched(buffer)) {
					index -= SHORT_LENGTH;
					map.position(index);
					// read content length field
					int length = map.getShort();
					if (length > 0) {
						index -= length;
						map.position(index);
						// read content bytes
						byte[] bytesComment = new byte[length];
						map.get(bytesComment);
						return new String(bytesComment, UTF_8);
					}
				}
			} finally {
				if (map != null) {
					map.clear();
				}
				if (raf != null) {
					raf.close();
				}
			}
			return null;
		}

		public static void writeMarket(final File file, final String market) throws IOException {
			writeZipComment(file, market);
		}

		public static String readMarket(final File file) throws IOException {
			return readZipComment(file);
		}

		public static boolean verifyMarket(final File file, final String market) throws IOException {
			return market.equals(readMarket(file));
		}

		public static void println(String msg) {
			System.out.println(msg);
		}

		public static void printErr(String msg) {
			System.err.println(msg);
		}

		public static List<String> parseMarkets(final File file) throws IOException {
			final List<String> markets = new ArrayList<String>();
			// 解决因为utf-8有无BOM引起的乱码问题
			BufferedReader br = new BufferedReader(new UnicodeReader(new FileInputStream(file), UTF_8));
			String line = null;
			int lineNo = 1;
			while ((line = br.readLine()) != null) {
				String parts[] = line.split("#");
				if (parts.length > 0) {
					final String market = parts[0].trim();
					if (market.length() > 0 && !markets.contains(market)) {
						markets.add(market);
					}
				}
				++lineNo;
			}
			br.close();
			return markets;
		}

		public static void copyFile(File src, File dest) throws IOException {
			if (dest.exists()) {
				dest.delete();
			}
			dest.createNewFile();
			FileChannel source = null;
			FileChannel destination = null;
			try {
				source = new FileInputStream(src).getChannel();
				destination = new FileOutputStream(dest).getChannel();
				destination.transferFrom(source, 0, source.size());
			} finally {
				if (source != null) {
					source.close();
				}
				if (destination != null) {
					destination.close();
				}
			}
		}

		public static boolean deleteDir(File dir) {
			File[] files = dir.listFiles();
			if (files == null || files.length == 0) {
				return false;
			}
			for (File file : files) {
				if (file.isDirectory()) {
					deleteDir(file);
				} else {
					file.delete();
				}
			}
			return true;
		}

		public static String getExtension(final String fileName) {
			int dot = fileName.lastIndexOf(".");
			if (dot > 0) {
				return fileName.substring(dot + 1);
			} else {
				return null;
			}
		}

		public static String getBaseName(final String fileName) {
			int dot = fileName.lastIndexOf(".");
			if (dot > 0) {
				return fileName.substring(0, dot);
			} else {
				return fileName;
			}
		}
	}

	private static class UnicodeReader extends Reader {
		PushbackInputStream internalIn;
		InputStreamReader internalIn2 = null;
		String defaultEnc;

		private static final int BOM_SIZE = 4;

		/**
		 *
		 * @param in
		 *            inputstream to be read
		 * @param defaultEnc
		 *            default encoding if stream does not have BOM marker. Give
		 *            NULL to use system-level default.
		 */
		UnicodeReader(InputStream in, String defaultEnc) {
			internalIn = new PushbackInputStream(in, BOM_SIZE);
			this.defaultEnc = defaultEnc;
		}

		public String getDefaultEncoding() {
			return defaultEnc;
		}

		/**
		 * Get stream encoding or NULL if stream is uninitialized. Call init()
		 * or read() method to initialize it.
		 */
		public String getEncoding() {
			if (internalIn2 == null)
				return null;
			return internalIn2.getEncoding();
		}

		/**
		 * Read-ahead four bytes and check for BOM marks. Extra bytes are unread
		 * back to the stream, only BOM bytes are skipped.
		 */
		protected void init() throws IOException {
			if (internalIn2 != null)
				return;

			String encoding;
			byte bom[] = new byte[BOM_SIZE];
			int n, unread;
			n = internalIn.read(bom, 0, bom.length);

			if ((bom[0] == (byte) 0x00) && (bom[1] == (byte) 0x00) && (bom[2] == (byte) 0xFE)
					&& (bom[3] == (byte) 0xFF)) {
				encoding = "UTF-32BE";
				unread = n - 4;
			} else if ((bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE) && (bom[2] == (byte) 0x00)
					&& (bom[3] == (byte) 0x00)) {
				encoding = "UTF-32LE";
				unread = n - 4;
			} else if ((bom[0] == (byte) 0xEF) && (bom[1] == (byte) 0xBB) && (bom[2] == (byte) 0xBF)) {
				encoding = "UTF-8";
				unread = n - 3;
			} else if ((bom[0] == (byte) 0xFE) && (bom[1] == (byte) 0xFF)) {
				encoding = "UTF-16BE";
				unread = n - 2;
			} else if ((bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE)) {
				encoding = "UTF-16LE";
				unread = n - 2;
			} else {
				// Unicode BOM mark not found, unread all bytes
				encoding = defaultEnc;
				unread = n;
			}
			// System.out.println("read=" + n + ", unread=" + unread);

			if (unread > 0)
				internalIn.unread(bom, (n - unread), unread);

			// Use given encoding
			if (encoding == null) {
				internalIn2 = new InputStreamReader(internalIn);
			} else {
				internalIn2 = new InputStreamReader(internalIn, encoding);
			}
		}

		public void close() throws IOException {
			init();
			internalIn2.close();
		}

		public int read(char[] cbuf, int off, int len) throws IOException {
			init();
			return internalIn2.read(cbuf, off, len);
		}
	}
}