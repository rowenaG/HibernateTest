/*
 * Copyright 2018 Leidos
 */

package com.leidos.model;

@SuppressWarnings( "ClassWithoutLogger" )
public class Part
{
  private int uid = -1;
  private String name = "";
  private int order = 1;

  public int getOrder()
  {
    return order;
  }

  public void setOrder( int order )
  {
    this.order = order;
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
}
