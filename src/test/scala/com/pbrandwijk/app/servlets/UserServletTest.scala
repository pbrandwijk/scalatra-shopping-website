package com.pbrandwijk.app.servlets

import net.liftweb.json._
import org.scalatra.test.scalatest._
import com.pbrandwijk.app.Model

class UserServletTest extends ScalatraFunSuite {

  addServlet(classOf[UserServlet], "/users/*")
  addServlet(classOf[ProductServlet], "/products/*")

  val invalidJson = """{ "id": "book001", "descr"""
  val incompleteUserBody = """{ "email": "johndoe@example.com", "name": "John Doe" }"""
  val addUser1Body = """{ "email": "johndoe@example.com", "name": "John Doe", "bankAccount": "9876543210" }"""
  val addItemToUserCart1Body = """{ "email": "johndoe@example.com", "id": "book001", "quantity": "1" }"""
  val addItemToUserCart2Body = """{ "email": "johndoe@example.com", "id": "book001", "quantity": "3" }"""
  val addItemToUserCart3Body = """{ "email": "johndoe@example.com", "id": "book002", "quantity": "20" }"""
  val addItemToUserCart4Body = """{ "email": "johndoe@example.com", "id": "book002", "quantity": "2" }"""
  val addItemToUserCart5Body = """{ "email": "johndoe@example.com", "id": "book002", "quantity": "two" }"""
  val checkoutUser1Body = """{ "email": "johndoe@example.com", "address": "Mainstreet 1, Johnsville" }"""
  val user1email = "johndoe@example.com"
  val user1name = "John Doe"
  val book1id = "book001"
  val book1title = "The Da Vinci Code"
  val book2id = "book002"


  /* Test for GET /users */

  test("GET /users/ on UserServlet should return status 200") {
    get("/users/") {
      status should equal (200)
      Model.users.size should equal (0)
    }
  }

  /* Tests for POST /users/addUser */

  test("POST /users/addUser on UserServlet with invalid JSON should do nothing and give error " +
    "message in header") {
    submit("POST", "/users/addUser", Seq.empty, Seq.empty, invalidJson) {
      status should equal (200)
      Model.users.size should equal (0)
      header.get("ACK") should not be None
      header.get("ACK").get should equal ("Request body could not be parsed into JSON")
    }
  }

  test("POST /users/addUser on UserServlet with incomplete JSON should do nothing and give error " +
    "message in header") {
    submit("POST", "/users/addUser", Seq.empty, Seq.empty, incompleteUserBody) {
      status should equal (200)
      Model.users.size should equal (0)
      header.get("ACK") should not be None
      header.get("ACK").get should equal ("JSON cannot be mapped to user model")
    }
  }

  test("POST /users/addUser on UserServlet should add user to model") {
    submit("POST", "/users/addUser", Seq.empty, Seq.empty, addUser1Body) {
      status should equal(200)
      Model.users.size should equal (1)
      Model.users.get(user1email) should not be (None)
      Model.users.get(user1email).get.name should be (user1name)
      Model.users.get(user1email).get.cart.size should equal (0)
    }
  }

  test("GET /users/ on UserServlet should give correct JSON when user was added") {
    get("/users/") {
      status should equal (200)
      body should include (user1name)

      // check that the JSON in the body contains the right email address
      implicit val formats = DefaultFormats
      val parsedBody = parse(body)
      val email = (parsedBody \ "_1").values
      email should be (user1email)
    }
  }

  /* Tests for POST /users/addItemToUserCart */

  test("POST /users/addItemToUserCart on UserServlet with invalid JSON should do nothing and give error " +
    "message in header") {
    submit("POST", "/users/addItemToUserCart", Seq.empty, Seq.empty, invalidJson) {
      status should equal (200)
      header.get("ACK") should not be None
      header.get("ACK").get should equal ("Request body could not be parsed into JSON")
    }
  }

  test("POST /users/addItemToUserCart on UserServlet with incomplete JSON should do nothing and give error " +
    "message in header") {
    submit("POST", "/users/addItemToUserCart", Seq.empty, Seq.empty, incompleteUserBody) {
      status should equal (200)
      header.get("ACK") should not be None
      header.get("ACK").get should equal ("JSON cannot be mapped to item model")
    }
  }

  test("POST /users/addItemToUserCart on UserServlet should add item and quantity to user in model " +
    "and should decrease the stock of the product with the quantity") {
    submit("POST", "/users/addItemToUserCart", Seq.empty, Seq.empty, addItemToUserCart1Body) {
      status should equal (200)
      Model.users.get(user1email).get.cart.size should equal (1)
      Model.users.get(user1email).get.cart.get(book1id) should not be (None)
      Model.users.get(user1email).get.cart.get(book1id).get should equal (1)
      Model.products.get(book1id).get.stock should equal (9)
    }
  }

  test("POST /users/addItemToUserCart on UserServlet when done twice should not doubly decrease the " +
    "product in stock") {
    submit("POST", "/users/addItemToUserCart", Seq.empty, Seq.empty, addItemToUserCart1Body) {
      status should equal (200)
      Model.users.get(user1email).get.cart.size should equal (1)
      Model.users.get(user1email).get.cart.get(book1id) should not be (None)
      Model.users.get(user1email).get.cart.get(book1id).get should equal (1)
      Model.products.get(book1id).get.stock should equal (9)
    }
  }

  test("POST /users/addItemToUserCart on UserServlet when done with same item but different quantity should " +
    "update the quantity on user cart and in product stock") {
    submit("POST", "/users/addItemToUserCart", Seq.empty, Seq.empty, addItemToUserCart2Body) {
      status should equal (200)
      Model.users.get(user1email).get.cart.size should equal (1)
      Model.users.get(user1email).get.cart.get(book1id) should not be (None)
      Model.users.get(user1email).get.cart.get(book1id).get should equal (3)
      Model.products.get(book1id).get.stock should equal (7)
    }
  }

  test("POST /users/addItemToUserCart on UserServlet when done with a quantity bigger that the stock" +
    "should not do anything") {
    submit("POST", "/users/addItemToUserCart", Seq.empty, Seq.empty, addItemToUserCart3Body) {
      status should equal (200)
      Model.users.get(user1email).get.cart.size should equal (1)
      Model.users.get(user1email).get.cart.get(book2id) should be (None)
      Model.products.get(book2id).get.stock should equal (5)
    }
  }

  test("POST /users/addItemToUserCart on UserServlet when done with a second item should add a " +
    "second item to the user's shopping cart") {
    submit("POST", "/users/addItemToUserCart", Seq.empty, Seq.empty, addItemToUserCart4Body) {
      status should equal (200)
      Model.users.get(user1email).get.cart.size should equal (2)
      Model.users.get(user1email).get.cart.get(book2id) should not be (None)
      Model.users.get(user1email).get.cart.get(book2id).get should be (2)
      Model.products.get(book2id).get.stock should equal (3)
    }
  }

  test("POST /users/addItemToUserCart on UserServlet when done an invalid value for quantity " +
    "should give an error message in header") {
    submit("POST", "/users/addItemToUserCart", Seq.empty, Seq.empty, addItemToUserCart5Body) {
      status should equal (200)
      header.get("ACK") should not be None
      header.get("ACK").get should equal ("Value for quantity cannot be parsed as an integer")
    }
  }

  /* Tests for POST /users/checkout */

  test("POST /users/checkout on UserServlet with invalid JSON should do nothing and give error " +
    "message in header") {
    submit("POST", "/users/checkout", Seq.empty, Seq.empty, invalidJson) {
      status should equal (200)
      header.get("ACK") should not be None
      header.get("ACK").get should equal ("Request body could not be parsed into JSON")
    }
  }

  test("POST /users/checkout on UserServlet with incomplete JSON should do nothing and give error " +
    "message in header") {
    submit("POST", "/users/checkout", Seq.empty, Seq.empty, incompleteUserBody) {
      status should equal (200)
      header.get("ACK") should not be None
      header.get("ACK").get should equal ("JSON cannot be mapped to checkout model")
    }
  }

  test("POST /users/checkout on UserServlet creates new order and cleans user's shopping cart") {
    submit("POST", "/users/checkout", Seq.empty, Seq.empty, checkoutUser1Body) {
      status should equal (200)
      body should equal ("""{ "orderNumber": 1, "totalPrice": 46.83}""")
      Model.users.get(user1email).get.cart.size should equal (0)
      Model.products.get(book1id).get.stock should equal (7)
      Model.products.get(book2id).get.stock should equal (3)
      Model.orders.size should equal (1)
      Model.orders.get(1).get.items.size should equal (2)
    }
  }

  test("GET /users/orders on UserServlet should return status 200") {
    get("/users/orders") {
      status should equal (200)
      body should include ("Johnsville")
    }
  }

  test("POST /users/checkout on UserServlet when cart is empty does nothing and gives 200 with error message in header") {
    submit("POST", "/users/checkout", Seq.empty, Seq.empty, checkoutUser1Body) {
      status should equal (200)
      header.get("ACK") should not be None
      header.get("ACK").get should include ("shopping cart is empty")
      Model.users.get(user1email).get.cart.size should equal (0)
      Model.products.get(book1id).get.stock should equal (7)
      Model.products.get(book2id).get.stock should equal (3)
      Model.orders.size should equal (1)
      Model.orders.get(1).get.items.size should equal (2)
    }
  }

}
