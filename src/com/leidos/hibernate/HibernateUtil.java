package com.leidos.hibernate;

/*
* $Revision:   1.14  $
* $Date:   Mar 20 2006 09:27:36  $
*
* Copyright Leidos
*
*/

import com.lmco.swis.util.logging.Logger;
import org.hibernate.*;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Does Hibernate things.
 */
@SuppressWarnings( "unused" )
public class HibernateUtil
{

  //public static int MAX_PARAMETER_LIST_SIZE = 2000;
  /* the largest size of a collection to be used in the Restriction.in() method */
  public static final int MAX_SAFE_IN_SIZE = 1900;

  @SuppressWarnings( "StaticNonFinalField" )
  private static SessionFactory sessionFactory = null;
  private static final ThreadLocal<Session> threadSession = new ThreadLocal<>();
  private static final ThreadLocal<Transaction> threadTransaction = new ThreadLocal<>();
  private static Metadata metadata = null;
  private static SessionFactoryImplementor sessionFactoryImplementor = null;
  private static SessionFactoryServiceRegistry serviceRegistry = null;

  private static final Logger logger = Logger.getLogger( HibernateUtil.class.getName() );

  private HibernateUtil()
  {
  }

  /**
   * Configure Hibernate Session Factory. Called once per JVM session
   */
  @SuppressWarnings( "StaticMethodOnlyUsedInOneClass" )
  public static void setSessionFactory( SessionFactory sessionFactory )
  {
    HibernateUtil.sessionFactory = sessionFactory;
  }

  public static SessionFactory getSessionFactory()
  {
    return sessionFactory;
  }

  public static void setMetadata( Metadata metadata )
  {
    HibernateUtil.metadata = metadata;
  }

  public static Metadata getMetadata()
  {
    return metadata;
  }

  public static Session getSession()
  {
    return threadSession.get();
  }

  /**
   * Obtain a Hibernate Session. Creates a new Session and associates it with this
   * thread. Only one Session is allowed per thread. A second call to this method
   * without first closing the session will cause an exception to be thrown.
   * <p/>
   * The user must clean up by calling <code>closeSession</code>
   *
   * @return a Hibernate Session
   * @throws HibernateException exception thrown
   */
  public static Session openSession() throws HibernateException
  {
    Session session = threadSession.get();

    // Open a new session if one does not exist

      if (null == session)
      {
        session = sessionFactory.openSession();
        threadSession.set( session );
      }
      else
      {
        throw new HibernateException( "Illegal attempt to open a second session" );
      }


    return session;
  }

  /**
   * Closes Hibernate Session associated with this thread (if present) and
   * clears the association. Also rolls back and unregisters any outstanding
   * Transaction. This should have happened in <code>commitTransaction</code>
   * or <code>rollbackTransation</code> methods if the calling code is well behaved.
   * But just in case it isn't... Any rollback failure is silent. Session
   * close failure is thrown as a ConnectorException.
   *
   * @throws HibernateException exception thrown
   */
  @SuppressWarnings( "MethodWithMoreThanThreeNegations" )
  public static void closeSession() throws HibernateException
  {
    // Get Session and Transaction
    Session session = threadSession.get();
    Transaction transaction = threadTransaction.get();

    // Check Session status and close

      // Belt and braces try/catch for Transaction
      //noinspection NestedTryStatement
      try
      {
        // Rollback Transaction
        if (null != transaction && TransactionStatus.COMMITTED != transaction.getStatus() &&
            TransactionStatus.ROLLED_BACK != transaction.getStatus())
        {
          transaction.rollback();
        }
      }
      catch (HibernateException ignored)
      {
        // Nothing to be done here if the rollback fails.
        // Still need to null threads and close session so carry on.
      }

      // Null out Transaction
      threadTransaction.set( null );

      // Null out Session
      threadSession.set( null );

      // Close session
      if (null != session )
      {
        session.clear();
        if ( session.isOpen() )
        {
          session.close();
        }
      }
  }

  /**
   * Start an new Transaction. Will join an existing Transaction if one has
   * already been started in this thread. If no Session is currently open then
   * one will be opened and registered.
   *
   * @throws HibernateException exception thrown
   */
  public static void beginTransaction() throws HibernateException
  {
    Transaction transaction = threadTransaction.get();
    // Start a new Transaction if not found

      if (null == transaction)
      {
        //tx = getSession().beginTransaction();
        Session session = threadSession.get();
        if (null == session)
        {
          session = openSession();
        }

        transaction = session.beginTransaction();
        threadTransaction.set( transaction );
      }

  }

  /**
   * Commit the current Transaction. If a Transaction has not been begun no action is taken.
   *
   * @throws HibernateException        Connector Exception thrown
   * @throws StaleObjectStateException Stale State Exception thrown
   */
  @SuppressWarnings( "StatementWithEmptyBody" )
  public static void commitTransaction() throws HibernateException
  {
    Transaction transaction = threadTransaction.get();

    // Check Transaction and commit
    try
    {
      if (null == transaction)
      {
        //logger.debug( "Transaction is set to NULL" );
      }
      else
      {
        //noinspection IfStatementWithIdenticalBranches
        if (TransactionStatus.COMMITTED == transaction.getStatus())
        {
          //logger.debug( "Transaction has already been committed" );
        }
        else if (TransactionStatus.ROLLED_BACK == transaction.getStatus())
        {
          //logger.debug( "Transaction has already been rolled backed" );
        }
        else
        {
          transaction.commit();
          //          logger.debug( "Transaction being committed, and Transaction set back to NULL" );
          threadTransaction.set( null );
        }
      }
    }
    catch (StaleObjectStateException sose)
    {
      logger.warn( "Stale object state", sose );
      throw sose;
    }
    catch (HibernateException he)
    {
      // Ensure we null out the Transaction

      rollbackTransaction();

      logger.error( "Could not roll back transaction", he );
    }
  }

  /**
   * Rollback current Transaction. Current Session is closed if a rollback
   * failure occurs.
   *
   * @throws HibernateException exception thrown
   */
  public static void rollbackTransaction() throws HibernateException
  {
    Transaction transaction = threadTransaction.get();

    try
    {
      threadTransaction.set( null );
      if (null != transaction && TransactionStatus.COMMITTED != transaction.getStatus() && TransactionStatus.ROLLED_BACK != transaction.getStatus())
      {
        transaction.rollback();
      }
    }
    catch (HibernateException he)
    {
      try
      {
        closeSession();
      }
      catch (HibernateException ignored)
      {
        // Report and Ignore
        logger.error( "Could not roll back transaction", he );
      }
    }
  }

  public static void setSessionFactoryImplementor( SessionFactoryImplementor sessionFactoryImplementor )
  {
    HibernateUtil.sessionFactoryImplementor = sessionFactoryImplementor;
  }

  public static void setServiceRegistry( SessionFactoryServiceRegistry serviceRegistry )
  {
    HibernateUtil.serviceRegistry = serviceRegistry;
  }

  public static SessionFactoryImplementor getSessionFactoryImplementor()
  {
    return sessionFactoryImplementor;
  }

  public static SessionFactoryServiceRegistry getServiceRegistry()
  {
    return serviceRegistry;
  }
}