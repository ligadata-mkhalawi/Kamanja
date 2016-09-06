import abc
from common.ModelInstance import ModelInstance
import json
import logging

class LoanTuple(ModelInstance): 
        """ Model LoanTuple will sum msg["a"] and msg["b"] """
        def execute(self, msg):
                """ 
                A real implementation would use the output fields to 
                determine what should be returned. 
                """
                pred = -1.49786052275209
                pred += float(msg["TIN_IMO_REASON_DebtCon"]) * -0.239668692936379
                pred += float(msg["TIN_IZR_JOB_Mgr"]) * 1.37932908138322
                pred += float(msg["TIN_IZR_JOB_Office"]) * 0.775391491255473
                pred += float(msg["TIN_IZR_JOB_Other"]) * 1.44907154775426
                pred += float(msg["TIN_IZR_JOB_ProfExe"]) * 1.40707813621254
                pred += float(msg["TIN_IZR_JOB_Sales"]) * 2.38272021895632
                pred += float (msg["TIN_IZR_JOB_Self"]) * 1.97738454381451
                pred += float (msg["R01_LOAN"]) * -1.98016770987421
                pred += float (msg["IMN_R01_MORTDUE"]) * -1.70893083156384
                pred += float (msg["IMN_R01_VALUE"]) * 46.8834972899893
                pred += float (msg["IMN_R01_YOJ"]) * -0.610183852644824
                pred += float (msg["IMN_R01_DEROG"]) * 4.47856184348009
                pred += float (msg["IMN_R01_DELINQ"]) * 7.37620839328494
                pred += float (msg["IMN_R01_CLAGE"]) * -6.85708989347486
                pred += float (msg["IMN_R01_NINQ"]) * 2.89418521536115
                pred += float (msg["IMN_R01_CLNO"]) * -0.296037079316927
                pred += float (msg["LOG6_DEROG"]) * 0.390988196099627
                pred += float (msg["LOG5_DELINQ"]) * 1.8255237489947
                pred += float (msg["LOG_VALUE"]) * -35.3593065292401
                outMsg = json.dumps({'rec_ID' : msg["rec_ID"],
                                     'PYTHON_RISK_SCORE' : pred})
                return outMsg

        def getInputFields(self):
                """The field names and their types needed by the model are returned to """
                """the python proxy (model stub communicating with this server). """
                """Feel free to just hard code the type info if that is best. """
                """The returned dictionaries are used by the python proxy to choose """
                """which fields from the associated messages(s) to send to the python server """
                """when the model is executed.  This is appropriate when the message contains"""
                """a thousand fields, but the model only uses five of them. """

                """As shown, conceivably the information could be configured in the model """
                """options. """

                self.logger.debug("Entered LoanTuple.getInputFields")
                modelOptions = super(LoanTuple, self).ModelOptions()
                inputFields = dict()
                if "InputTypeInfo" in modelOptions:
                        inputFields.update(modelOptions["InputTypeInfo"])
                else:
                        inputFields["recid"] = "Int"
                        inputFields["bad0"] = "Float"
                        inputFields["TIN_IMO_REASON_Debtcon"] = "Float"
                        inputFields["TIN_IZR_JOB_Mgr"] = "Float"
                        inputFields["TIN_IZR_JOB_Office"] = "Float"
                        inputFields["TIN_IZR_JOB_Other"] = "Float"
                        inputFields["TIN_IZR_JOB_ProfExe"] = "Float"
                        inputFields["TIN_IZR_JOB_Sales"] = "Float"
                        inputFields["TIN_IZR_JOB_Self"] = "Float"
                        inputFields["R01_LOAN"] = "Float"
                        inputFields["IMN_R01_MORTDUE"] = "Float"
                        inputFields["IMN_R01_YOJ"] = "Float"
                        inputFields["IMN_R01_DEROG"] = "Float"
                        inputFields["IMN_R01_DELINQ"] = "Float"
                        inputFields["IMN_R01_CLAGE"] = "Float"
                        inputFields["IMN_R01_NINQ"] = "Float"
                        inputFields["IMN_R01_CLNO"] = "Float"
                        inputFields["LOG6_DEROG"] = "Float"
                        inputFields["LOG5_DELINQ"] = "Float"
                        inputFields["LOG_VALUE"] = "Float"

                return (inputFields)

