package ticket.booking.json

import spray.json.{RootJsonFormat, JsString, JsValue, DeserializationException}
import akka.http.scaladsl.model.DateTime

object DateJsonFormat extends RootJsonFormat[DateTime] {
  override def write(obj: DateTime) = JsString(obj.toIsoDateTimeString())
  override def read(json: JsValue) : DateTime = json match {
    case JsString(s) => DateTime.fromIsoDateTimeString(s).getOrElse(
      throw new DeserializationException("String does not follow Iso Date format.")
      )
    case _ => throw new DeserializationException("Type of date value has to be string.")
  }
}

