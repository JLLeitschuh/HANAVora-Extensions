package org.apache.spark.sql.sources

import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.catalyst.TableIdentifier
import org.apache.spark.sql.catalyst.plans.logical.view.{AbstractView, Persisted}
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan

import scala.reflect._

/** A provider for storing views and dropping them via a retrieved [[ViewHandle]] */
trait AbstractViewProvider[A <: AbstractView with Persisted] {
  val tag: ClassTag[A]

  def create(createViewInput: CreateViewInput): ViewHandle
}

/**
  * A handle of the view on provider side.
  *
  * Via this handle, the view can be dropped on provider side.
  */
trait ViewHandle {
  /** Drops the view from provider side. */
  def drop(): Unit

  /** The name of the view on provider side. */
  def name: String
}

abstract class BaseAbstractViewProvider[A <: AbstractView with Persisted: ClassTag]
  extends AbstractViewProvider[A] {
  val tag = implicitly[ClassTag[A]]
}

object AbstractViewProvider {
  def matcherFor(kind: ViewKind)(any: Any): Option[AbstractViewProvider[_]] = {
    val multiProvider = MultiAbstractViewProvider.matcherFor(kind)
    any match {
      case provider: AbstractViewProvider[_] if tagMatches(provider.tag) =>
        Some(provider)
      case multiProvider(provider) =>
        Some(provider)
      case _ => None
    }
  }

  private def tagMatches[A: ClassTag](tag: ClassTag[_]): Boolean = {
    classTag[A].runtimeClass.isAssignableFrom(tag.runtimeClass)
  }
}

case class CreateViewInput(
    sqlContext: SQLContext,
    plan: LogicalPlan,
    viewSql: String,
    options: Map[String, String],
    identifier: TableIdentifier,
    allowExisting: Boolean)

case class DropViewInput(
    sqlContext: SQLContext,
    options: Map[String, String],
    identifier: TableIdentifier,
    allowNotExisting: Boolean)
