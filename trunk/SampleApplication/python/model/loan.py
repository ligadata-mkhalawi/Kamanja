import abc
from common.ModelInstance import ModelInstance
import json
import logging
import  math 

class LoanTuple(ModelInstance): 
        """ Model LoanTuple will sum msg["a"] and msg["b"] """
        def execute(self, msg):
                """ 
                A real implementation would use the output fields to 
                determine what should be returned. 
                """
                pred = -1.49786052275209
                pred += float(msg["tin_imo_reason_debtcon"]) * -0.239668692936379
                pred += float(msg["tin_izr_job_mgr"]) * 1.37932908138322
                pred += float(msg["tin_izr_job_office"]) * 0.775391491255473
                pred += float(msg["tin_izr_job_other"]) * 1.44907154775426
                pred += float(msg["tin_izr_job_profexe"]) * 1.40707813621254
                pred += float(msg["tin_izr_job_sales"]) * 2.38272021895632
                pred += float (msg["tin_izr_job_self"]) * 1.97738454381451
                pred += float (msg["r01_loan"]) * -1.98016770987421
                pred += float (msg["imn_r01_mortdue"]) * -1.70893083156384
                pred += float (msg["imn_r01_value"]) * 46.8834972899893
                pred += float (msg["imn_r01_yoj"]) * -0.610183852644824
                pred += float (msg["imn_r01_derog"]) * 4.47856184348009
                pred += float (msg["imn_r01_delinq"]) * 7.37620839328494
                pred += float (msg["imn_r01_clage"]) * -6.85708989347486
                pred += float (msg["imn_r01_ninq"]) * 2.89418521536115
                pred += float (msg["imn_r01_clno"]) * -0.296037079316927
                pred += float (msg["log6_derog"]) * 0.390988196099627
                pred += float (msg["log5_delinq"]) * 1.8255237489947
                pred += float (msg["log_value"]) * -35.3593065292401
                pred = (math.exp(pred) / (1 + math.exp(pred)))
                        
                outMsg = json.dumps({'rec_id' : msg["rec_id"],
                                     'python_risk_score' : pred})
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
                        inputFields["rec_id"] = "Int"
                        inputFields["bad0"] = "Float"
                        inputFields["tin_imo_reason_debtcon"] = "Float"
                        inputFields["tin_izr_job_mgr"] = "Float"
                        inputFields["tin_izr_job_office"] = "Float"
                        inputFields["tin_izr_job_other"] = "Float"
                        inputFields["tin_izr_job_profexe"] = "Float"
                        inputFields["tin_izr_job_sales"] = "Float"
                        inputFields["tin_izr_job_self"] = "Float"
                        inputFields["r01_loan"] = "Float"
                        inputFields["imn_r01_mortdue"] = "Float"
                        inputFields["imn_r01_yoj"] = "Float"
                        inputFields["imn_r01_derog"] = "Float"
                        inputFields["imn_r01_value"] = "Float"
                        inputFields["imn_r01_delinq"] = "Float"
                        inputFields["imn_r01_clage"] = "Float"
                        inputFields["imn_r01_ninq"] = "Float"
                        inputFields["imn_r01_clno"] = "Float"
                        inputFields["log6_derog"] = "Float"
                        inputFields["log5_delinq"] = "Float"
                        inputFields["log_value"] = "Float"

                return (inputFields)

