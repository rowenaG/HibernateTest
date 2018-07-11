/*
 * Copyright 2018 Leidos
 */

package com.leidos.model;

@SuppressWarnings( "ClassWithoutLogger" )
public class Section
{
  private int uid = -1;
  private String name = null;
  private Content content = null;
  private int contentId = -1;
  private int partId = -1;

  public int getPartId()
  {
    return partId;
  }

  public void setPartId( int partId )
  {
    this.partId = partId;
  }

  public int getContentId()
  {
    if ( null == content )
    {
      return contentId;
    }
    else
    {
      return content.getUid();
    }
  }

  public void setContentId( int contentId )
  {
    this.contentId = contentId;
  }

  public String getName()
  {
    return name;
  }

  public void setName( String name )
  {
    this.name = name;
  }

  public int getUid()
  {
    return uid;
  }

  public void setUid( int uid )
  {
    this.uid = uid;
  }

  public Content getContent()
  {
    return content;
  }

  public void setContent( Content content )
  {
    this.content = content;
  }
}
