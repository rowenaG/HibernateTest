/*
 * Copyright 2018 Leidos
 */

package com.leidos.hibernate;

import com.leidos.model.Content;
import com.leidos.model.Part;
import com.leidos.model.Section;
import com.lmco.swis.util.logging.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.query.Query;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.EnumSet;
import java.util.List;

@SuppressWarnings( "ClassWithoutLogger" )
public class HibernateTest
{
  private static final String LOGGER_CONFIG = "conf/log4j2.xml";
  private boolean mSessionFactorySet = false;
  private SchemaExport mSchemaExport = null;
  private EnumSet<TargetType> targetTypes = EnumSet.of( TargetType.DATABASE );
  private static final String configFile = "conf/hibernate.cfg.xml";

  @Test
  public void testHibernate()
  {
    setupData();
    runTest();
  }

  private void setupData()
  {
    Session hibernateSession = HibernateUtil.openSession();
    HibernateUtil.beginTransaction();

    for (int i = 1; 5 >= i; i++ )
    {
      Part part = new Part();
      part.setName( "Part " + i);
      part.setOrder( i );

      hibernateSession.saveOrUpdate( part );

      for ( int j=1; 5 >= j; j++ )
      {
        Section section = new Section();
        section.setName( "Section " + i + "." + j );
        section.setPartId( part.getUid() );

        Content content = new Content();
        content.setContent( "Content for " + section.getName() );
        hibernateSession.saveOrUpdate( content );
        section.setContent( content );
        hibernateSession.saveOrUpdate( section );
      }
    }
    HibernateUtil.commitTransaction();
    HibernateUtil.closeSession();
  }

  private void runTest()
  {
    Session hibernateSession = HibernateUtil.openSession();
    HibernateUtil.beginTransaction();

    Query<Part> query = hibernateSession.createQuery( "from Part", Part.class );
    List<Part> partList = query.list();
    for ( Part part : partList )
    {
      Part newPart = new Part();
      newPart.setName( part.getName() );
      newPart.setOrder( part.getOrder() );
      hibernateSession.saveOrUpdate( newPart );

      Query<Section> sectionQuery = hibernateSession.createQuery( "from Section where partId = " + part.getUid(), Section.class );
      List<Section> sectionList = sectionQuery.list();
      for ( Section section : sectionList )
      {
        Query<Content> contentQuery = hibernateSession.createQuery( "from Content where uid = " + section.getContentId(), Content.class );
        Content content = contentQuery.getSingleResult();

        if ( null != content )
        {
          Content newContent = new Content();
          newContent.setContent( content.getContent() );
          hibernateSession.saveOrUpdate( newContent );

          Section newSection = new Section();
          newSection.setName( section.getName() );
          newSection.setContent( newContent );
          newSection.setPartId( newPart.getUid() );
          hibernateSession.saveOrUpdate( newSection );
        }
      }
    }
    HibernateUtil.commitTransaction();
    HibernateUtil.closeSession();
  }

  @Before
  public void setUp() throws Exception
  {
    Logger.configure( HibernateTest.class.getResource( LOGGER_CONFIG ) );
    //noinspection AssignmentToStaticFieldFromInstanceMethod
    Logger logger = Logger.getLogger( HibernateTest.class.getName() );

    /* set up the session factory */
    if (!mSessionFactorySet)
    {
      //noinspection CatchGenericClass
      try
      {
        BootstrapServiceRegistry bootstrapRegistry = new BootstrapServiceRegistryBuilder()
          .applyIntegrator( new UnitTestIntegrator() ).build();

        URL configFileURL = HibernateTest.class.getResource( configFile );
        final ServiceRegistry
          serviceRegistry = new StandardServiceRegistryBuilder( bootstrapRegistry ).configure( configFileURL ).build();
        MetadataSources metadataSources = new MetadataSources( serviceRegistry );
        Configuration configuration = new Configuration( metadataSources );
        SessionFactory sessionFactory = configuration.buildSessionFactory( serviceRegistry );
        logger.debug("Session built");
        HibernateUtil.setSessionFactory(sessionFactory);
        mSessionFactorySet = true;
      }
      catch (HibernateException e)
      {
        logger.error( "Hibernate Exception thrown", e );
        Assert.fail( "org.hibernate.HibernateException thrown" + e.getMessage() );
      }
      catch (Throwable t)
      {
        logger.error( "Generic Exception thrown", t );
        Assert.fail( "Problem setting SessionFactory in setUp(): " + t.getMessage() );
      }
    }

    logger.debug("Setup schema export");
    mSchemaExport = new SchemaExport();
    mSchemaExport.create( targetTypes, HibernateUtil.getMetadata() );

    logger.debug("Setup Complete");
  }

  @After
  public void tearDown() throws Exception
  {
    mSchemaExport.drop( targetTypes, HibernateUtil.getMetadata());
    //noinspection AssignmentToNull
    mSchemaExport = null; // kill it.
  }

  private class UnitTestIntegrator implements Integrator
  {
    @Override
    public void integrate( Metadata metadata, SessionFactoryImplementor sessionFactoryImplementor,
                           SessionFactoryServiceRegistry sessionFactoryServiceRegistry )
    {
      HibernateUtil.setMetadata( metadata );
      HibernateUtil.setSessionFactoryImplementor( sessionFactoryImplementor );
      HibernateUtil.setServiceRegistry( sessionFactoryServiceRegistry );
    }

    @Override
    public void disintegrate( SessionFactoryImplementor sessionFactoryImplementor,
                              SessionFactoryServiceRegistry sessionFactoryServiceRegistry )
    {
      // nothing to do
    }
  }
}
