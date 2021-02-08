import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore

# Use a service account
cred = credentials.Certificate('things/cardau-e7c80-firebase-adminsdk-spds3-f66eed6982.json')
firebase_admin.initialize_app(cred)

# Get a reference to the database service
db = firestore.client()

directory = "cards"
cards = ["black", "white"]
languages = ["italiano", "english"]

# Load white cards
for card_set in cards:
    for language in languages:
        # Define card set filename
        filename = directory + "/" + card_set + "/" + language + ".txt"
        print("Loading cards from: ", filename)
        # Get file text
        card_texts = open(filename, encoding='utf8', mode="r")

        # Define a cursor for operation on db
        for i, card_text in enumerate(card_texts.readlines()):
            # Insert into DB
            doc_ref = db.collection(language+"-"+card_set).document(str(i))
            doc_ref.set({u'text': card_text,
                         u'usage': 0}
                        )
