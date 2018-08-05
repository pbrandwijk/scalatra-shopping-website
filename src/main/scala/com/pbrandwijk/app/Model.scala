package com.pbrandwijk.app

import com.pbrandwijk.app.models._
import org.slf4j.LoggerFactory

import scala.collection.mutable._

/**
  * Object that contains the model of the data in the application.
  */
object Model {

  // create logger for class
  val logger =  LoggerFactory.getLogger(getClass)

  // order numbers are assigned automatically, starting at 1 and incrementing with every order
  var orderReference = 1
  // products are stored in a mutable HashMap
  var products = new HashMap[String, Product]
  // users are stored in a mutable HashMap
  var users = new HashMap[String, User]
  // orders are stored in a mutable HashMap
  var orders = new HashMap[Int, Order]

  /**
    * Add a product to the model.
    *
    * If the id is already present, then overwrite the existing product.
    *
    * @param product The product to add
    * @return Either the product if added successfully, or an error message if not
    */
  def addProduct(product: Product): Either[String, Product] = {

    if ((products get product.id) == None) {
      products += (product.id -> product)
      logger.info("Added new product: " + product)
    }
    else {
      products -= product.id
      products += (product.id -> product)
      logger.info("Updated product: " + product)
    }
    logger.debug("All products: " + products)
    return Right(product)
  }

  /**
    * Add a user to the model.
    *
    * If the email address is already present, then do nothing.
    *
    * @param user The user to add
    * @return Either the user if added successfully, or an error message if not
    */
  def addUser(user: User): Either[String, User] = {

    if ((users get user.email) == None) {
      users += (user.email -> user)
      logger.info("Added new user: " + user)
      return Right(user)
    }
    else {
      return Left("User already present")
    }
  }

  /**
    * Add an item and quantity to a user's shopping chart. Also remove the requested quantity from the
    * product's stock.
    *
    * If either there is no user object in the model for the given email address or no product for the item id,
    * then do nothing.
    * If there are not enough items of the product in stock, then do nothing.
    * If the chart of the user already has an entry for the given product id, then update the quantity on the chart
    * and in the product stock.
    *
    * @param email The uniquely identifying user email address
    * @param id The uniquely identifying product id
    * @param quantity The quantity of the given item to be added
    * @return Either the user if chart updated successfully, or an error message if not
    */
  def addItemToUserChart(email: String, id: String, quantity: Int): Either[String, User] = {

    val user = users get email
    val product = products get id

    // check if user and item exist
    if (user == None || product == None) {
      return Left("Given user or item does not exist")
    }

    // check that the product is sufficiently in stock
    if (quantity > product.get.stock) {
      return Left("Not enough items of product in stock")
    }

    // check if user already has this item on their chart
    val itemOnChart = user.get.chart.get(id)
    // if so, update product stock and chart
    if (itemOnChart != None) {

      // calculate quantity difference
      val qDiff = quantity - itemOnChart.get

      // update product stock
      product.get.stock -= qDiff

      // update quantity on user chart
      user.get.addItemToChart(id, quantity)
    }
    else {
      // add the item and quantity to the stock
      user.get.addItemToChart(id, quantity)

      // remove the quantity from the product in stock
      product.get.stock -= quantity
    }

    // return the user with updated chart
    return Right(user.get)
  }

  /**
    * Add an order to the orders list with an order number. Increase the order number for the next order. Clean the
    * user's shopping chart after placing the order.
    *
    * If the user does not exist, do nothing and return error message.
    * If the shopping chart of the user is empty, do nothing and return error message.
    *
    * @param email The uniquely identifying user email address
    * @param address The shipping address to which to ship the order
    * @return The order number and total price if checkout succeeded, else an error message
    */
  def addOrder(email: String, address: String): Either[String,(Int, Double)] = {

    // check if user exists or shopping chart is empty
    val userOption = users.get(email)
    if (userOption.isEmpty || userOption.get.chart.isEmpty) {
      return Left("User does not exist or user's shopping chart is empty")
    }

    // get a clone of the shopping chart for this user
    val user = userOption.get
    val items = user.chart.clone()

    // go over all items to calculate the total price
    var total : Double = 0.0
    items.foreach {
      case(id, quantity) =>
        // find product price
        val productPrice = products.get(id).get.price
        // multiply product price by quantity and add to total
        total += productPrice * quantity
    }
    logger.info("Calculated total price of order: " + total)

    // assemble order object
    val order = new Order(email, address, total, items)

    // set the order number to the current reference
    val orderNumber = orderReference

    // store the order
    orders += (orderNumber -> order)

    // increment the global order reference to set it up for the next item
    orderReference += 1

    // clean the shopping chart of the user
    user.chart = new HashMap[String, Int]

    return Right(orderNumber, total)
  }

}
