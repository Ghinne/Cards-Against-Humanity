class CardsReader:
    """Class that extract cards from pdf converted file."""

    def __init__(self, file_path):
        """Class constructor."""
        self.__file_path = file_path
        self.__cards = list()
        self.__not_cards_strings = ["Cards Against Humanity", "*Nome@Scelta*"]
        self.__max_len = 80

    def extract_subs(self, dot_at_end=False, check_for_underscore=False, uniform_underscores=False):
        """Method that open all files in raw folder and invoke cleaner."""
        with open(self.__file_path, encoding="latin-1", mode="r") as f:
            self.__clean_text(f.read(), dot_at_end, check_for_underscore, uniform_underscores)

    def __clean_text(self, raw_text, dot_at_end, check_for_underscore, uniform_underscores):
        """Method that clean raw text."""
        split_text = raw_text.split('\n')
        it = iter(split_text)

        string = next(it)
        card = list()
        while True:
            if string != "":
                card.append(string)
            else:
                if card:
                    joined = ' '.join(card)
                    if self.__is_card(joined):
                        if dot_at_end:
                            joined = joined + "."
                        if uniform_underscores:
                            joined = joined.replace("_____", "__")
                        if check_for_underscore:
                            if joined.count("__") == 0:
                                joined = joined + " __ ."
                        if len(joined) < self.__max_len:
                            self.__cards.append(joined)
                card.clear()
            try:
                # Get the next string
                string = next(it)
            except StopIteration:
                if card:
                    joined = ' '.join(card)
                    if self.__is_card(joined):
                        if dot_at_end:
                            joined = joined + "."
                        if uniform_underscores:
                            joined = joined.replace("_____", "__")
                        if check_for_underscore:
                            if joined.count("__") == 0:
                                joined = joined + " __ ."
                        if len(joined) < self.__max_len:
                            self.__cards.append(joined)
                break

    def __is_card(self, card):
        """Method that exclude not good cards."""
        for s in self.__not_cards_strings:
            if s in card:
                return False
        return True

    def write_on_file(self, output_filepath):
        """Method that write clean text and dict on file."""
        new_file = open(output_filepath, encoding="latin-1", mode="w")
        new_file.write('\n'.join(self.__cards))
        new_file.close()
