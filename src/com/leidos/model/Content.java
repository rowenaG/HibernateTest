/*
 * Copyright 2018 Leidos
 */

package com.leidos.model;

import com.leidos.hibernate.HibernateUtil;
import org.hibernate.Hibernate;

import java.sql.Clob;
import java.sql.SQLException;

@SuppressWarnings( "ClassWithoutLogger" )
public class Content
{
  private int uid = -1;
  private String content = "";
  private Clob clob;

  public Clob getContentClob()
  {
    if ( null != content )
    {
      clob = Hibernate.getLobCreator( HibernateUtil.getSession() ).createClob( content );
    }

    return clob;
  }

  public void setContentClob( Clob content ) throws SQLException
  {
    clob = content;

    this.content = content.getSubString( 1L, Long.valueOf( content.length() ).intValue() );
  }

  public int getUid()
  {
    return uid;
  }

  public void setUid( int uid )
  {
    this.uid = uid;
  }

  public String getContent()
  {
    return content;
  }

  public void setContent( String content )
  {
    this.content = content;
  }
}
