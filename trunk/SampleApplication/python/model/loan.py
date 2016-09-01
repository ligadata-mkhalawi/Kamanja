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
                                     'BAD' : msg["BAD"],
                                     'TIN_IMO_REASON_DebtCon' : msg["TIN_IMO_REASON_DebtCon"],
                                     'TIN_IMO_REASON_HomeImp' : msg["TIN_IMO_REASON_HomeImp"],
                                     'TIN_IZR_JOB_Mgr' : msg["TIN_IZR_JOB_Mgr"],
                                     'TIN_IZR_JOB_Office' : msg["TIN_IZR_JOB_Office"],
                                     'TIN_IZR_JOB_Other' : msg["TIN_IZR_JOB_Other"],
                                     'TIN_IZR_JOB_ProfExe' : msg["TIN_IZR_JOB_ProfExe"],
                                     'TIN_IZR_JOB_Sales' : msg["TIN_IZR_JOB_Sales"],
                                     'TIN_IZR_JOB_Self' : msg["TIN_IZR_JOB_Self"],
                                     'TIN_IZR_JOB_Missing' : msg["TIN_IZR_JOB_Missing"],
                                     'R01_LOAN' : msg["R01_LOAN"],
                                     'IMN_R01_MORTDUE' : msg["IMN_R01_MORTDUE"],
                                     'IMN_R01_VALUE' : msg["IMN_R01_VALUE"],
                                     'IMN_R01_YOJ' : msg["IMN_R01_YOJ"],
                                     'IMN_R01_DEROG' : msg["IMN_R01_DEROG"],
                                     'IMN_R01_DELINQ' : msg["IMN_R01_DELINQ"],
                                     'IMN_R01_CLAGE' : msg["IMN_R01_CLAGE"],
                                     'IMN_R01_NINQ' : msg["IMN_R01_NINQ"],
                                     'IMN_R01_CLNO' : msg["IMN_R01_CLNO"],
                                     'LOG6_DEROG' : msg["LOG6_DEROG"],
                                     'LOG5_DELINQ' : msg["LOG5_DELINQ"],
                                     'LOG_VALUE' : msg["LOG_VALUE"],
                                     'PYTHON_RISK_SCORE' : pred})
                return outMsg

