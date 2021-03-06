package com.arialyy.aria.ftp;

import aria.apache.commons.net.ftp.FTPSClient;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Locale;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocket;

public class SSLSessionReuseFTPSClient extends FTPSClient {

  SSLSessionReuseFTPSClient(boolean b, SSLContext context) {
    super(b, context);
  }

  // adapted from:
  // https://trac.cyberduck.io/browser/trunk/ftp/src/main/java/ch/cyberduck/core/ftp/FTPClient.java
  @Override
  protected void _prepareDataSocket_(final Socket socket) throws IOException {
    if (socket instanceof SSLSocket) {
      // Control socket is SSL
      final SSLSession session = ((SSLSocket) _socket_).getSession();
      if (session.isValid()) {
        final SSLSessionContext context = session.getSessionContext();
        try {
          //final Field sessionHostPortCache = context.getClass().getDeclaredField("sessionHostPortCache");
          final Field sessionHostPortCache =
              context.getClass().getDeclaredField("sessionsByHostAndPort");
          sessionHostPortCache.setAccessible(true);
          final Object cache = sessionHostPortCache.get(context);
          final Method method =
              cache.getClass().getDeclaredMethod("put", Object.class, Object.class);
          method.setAccessible(true);
          method.invoke(cache, String.format("%s:%s", socket.getInetAddress().getHostName(),
              String.valueOf(socket.getPort())).toLowerCase(Locale.ROOT), session);
          method.invoke(cache, String.format("%s:%s", socket.getInetAddress().getHostAddress(),
              String.valueOf(socket.getPort())).toLowerCase(Locale.ROOT), session);
        } catch (NoSuchFieldException e) {
          throw new IOException(e);
        } catch (Exception e) {
          throw new IOException(e);
        }
      } else {
        throw new IOException("Invalid SSL Session");
      }
    }
  }
}