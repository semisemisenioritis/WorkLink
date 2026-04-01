from fastapi import FastAPI
import random
import smtplib

app = FastAPI()

otp_store = {}

@app.post("/send-otp")
def send_otp(email: str):

    otp = str(random.randint(100000,999999))
    otp_store[email] = otp

    sender = "divyangisingh2004@gmail.com"
    password = "uvydfscycynjverc"

    message = f"Subject: Your OTP\n\nYour OTP is {otp}"

    with smtplib.SMTP_SSL("smtp.gmail.com",465) as server:
        server.login(sender,password)
        server.sendmail(sender,email,message)

    return {"status":"sent"}

@app.post("/verify-otp")
def verify(email:str,otp:str):

    if otp_store.get(email)==otp:
        return {"status":"verified"}

    return {"status":"failed"}