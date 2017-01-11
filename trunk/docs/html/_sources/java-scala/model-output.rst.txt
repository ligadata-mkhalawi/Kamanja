
.. _java-scala-guide-model-output:

Creating Model Output
---------------------

There are two ways of creating model output:

- Implementing ModelResultBase
- Using a simple result object.

Implementing ModelResultBase
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

First, the hard way – implementing ModelResultBase:

::

  class LowBalanceAlertResult extends ModelResultBase {
    var custId: Long = 0;
    var branchId: Int = 0;
    var accNo: Long = 0;
    var curBalance: Double = 0
    var alertType: String = ""
    var triggerTime: Long = 0

    def withCustId(cId: Long): LowBalanceAlertResult = {
      custId = cId
      this
    }

    def withBranchId(bId: Int): LowBalanceAlertResult = {
      branchId = bId
      this
    }

    def withAccNo(aNo: Long): LowBalanceAlertResult = {
      accNo = aNo
      this
    }

    def withCurBalance(curBal: Double): LowBalanceAlertResult = {
      curBalance = curBal
      this
    }

    def withAlertType(alertTyp: String): LowBalanceAlertResult = {
      alertType = alertTyp
      this
    }

    def withTriggerTime(triggerTm: Long): LowBalanceAlertResult = {
      triggerTime = triggerTm
      this
    }

    override def toJson: List[org.json4s.JsonAST.JObject] = {
      val json = List(
      ("CustId" -&amp;gt; custId) ~
      ("BranchId" -&amp;gt; branchId) ~
      ("AccNo" -&amp;gt; accNo) ~
      ("CurBalance" -&amp;gt; curBalance) ~
      ("AlertType" -&amp;gt; alertType) ~
      ("TriggerTime" -&amp;gt; triggerTime))
      return json
    }

    override def toString: String = {
      compact(render(toJson))
    }

    override def get(key: String): Any = {
      if (key.compareToIgnoreCase("custId") == 0) return custId
      if (key.compareToIgnoreCase("branchId") == 0) return branchId
      if (key.compareToIgnoreCase("accNo") == 0) return accNo
      if (key.compareToIgnoreCase("curBalance") == 0) return curBalance
      if (key.compareToIgnoreCase("alertType") == 0) return alertType
      if (key.compareToIgnoreCase("triggerTime") == 0) return  triggerTime
      return null
    }

    override def asKeyValuesMap: Map[String, Any] = {
      val map = scala.collection.mutable.Map[String, Any]()
      map("custid") = custId
      map("branchid") = branchId
      map("accno") = accNo
      map("curbalance") = curBalance
      map("alerttype") = alertType
      map("triggertime") = triggerTime
      map.toMap
    }

    override def Deserialize(dis: DataInputStream): Unit = {
    // BUGBUG:: Yet to implement
    }

    override def Serialize(dos: DataOutputStream): Unit = {
    // BUGBUG:: Yet to implement
    }
  }
...
	
Using a simple result object
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

An easier way is to use the com.ligadata.KamanjaBase.MappedModelResultClass
that implements the withResults method.
Create an array of com.ligadata.KamanjaBase.Result objects
(Key[String]/Value[Any]):

::

  def withResults(res: Array[Result]): MappedModelResults = {
    if (res != null) {
      res.foreach(r => {
        results(r.name) = r.result
      })

    }
    this
  }

Create an array of com.ligadata.KamanjaBase.Result objects
(simple Key[String]/Value[Any]) and use this array
to build the output as follows:

::

  var resultArray = Array[Result]()......
    new LowBalanceAlertResult(). withResults(resultArray)


