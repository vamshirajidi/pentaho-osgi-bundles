/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/

package org.pentaho.osgi.impl;

import org.apache.mina.util.Base64;
import org.pentaho.osgi.api.CipherEncryptionService;
import org.pentaho.osgi.api.PasswordServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.spec.AlgorithmParameterSpec;

public class CipherEncryptionServiceImpl implements CipherEncryptionService {
  private static final Charset UTF_8 = StandardCharsets.UTF_8;
  private static final Logger LOGGER = LoggerFactory.getLogger( CipherEncryptionServiceImpl.class );
  private AlgorithmParameterSpec paramSpec;
  private SecretKey secretKey;

  public CipherEncryptionServiceImpl( String saltString, String algorithm, String encryptionKey, int iterations ) {
    if ( ( saltString == null ) || ( algorithm == null ) || ( encryptionKey == null ) ) {
      throw new RuntimeException( "Required properties not set - need Salt, algorithm and encryption key" );
    }
    if ( saltString.getBytes( UTF_8 ).length != 8 ) {
      throw new RuntimeException(
        "Salt must be 8 bytes when represented in UTF-8, found " + saltString.getBytes( UTF_8 ).length );
    }
    byte[] saltBytes = saltString.getBytes();
    paramSpec = new PBEParameterSpec( saltBytes, iterations );
    PBEKeySpec skeySpec = new PBEKeySpec( encryptionKey.toCharArray(), saltBytes, iterations );
    try {
      secretKey = SecretKeyFactory.getInstance( algorithm ).generateSecret( skeySpec );
    } catch ( Exception ex ) {
      LOGGER.error( "Error while creating secret key", ex );
      throw new RuntimeException( "Encryption requested not available" );
    }
  }

  @Override
  public String decrypt( String encryptedPassword ) throws PasswordServiceException {
    try {
      Cipher decCipher = Cipher.getInstance( secretKey.getAlgorithm() );
      decCipher.init( Cipher.DECRYPT_MODE, secretKey, paramSpec );
      byte[] toDecryptBytes = Base64.decodeBase64( encryptedPassword.getBytes() );
      byte[] decryptedBytes = decCipher.doFinal( toDecryptBytes );
      return new String( decryptedBytes, UTF_8 );
    } catch ( Exception ex ) {
      throw new PasswordServiceException( ex );
    }
  }

  @Override
  public String encrypt( String clearPassword ) throws PasswordServiceException {
    try {
      Cipher encCipher = Cipher.getInstance( secretKey.getAlgorithm() );
      encCipher.init( Cipher.ENCRYPT_MODE, secretKey, paramSpec );
      byte[] toEncryptBytes = clearPassword.getBytes( UTF_8 );
      byte[] encBytes = encCipher.doFinal( toEncryptBytes );
      byte[] base64Bytes = Base64.encodeBase64( encBytes );
      return new String( base64Bytes, UTF_8 );
    } catch ( Exception ex ) {
      throw new PasswordServiceException( ex );
    }
  }
}
