"""
NASA-Log extractor

@auth: Yu-Hsiang Fu
@date: 2018/03/26
"""
# --------------------------------------------------------------------------------
# 1.Main function
# --------------------------------------------------------------------------------
def main_function():

    folder_input = "input/"
    num_log = 50000

    print("Extract number of logs from NASA log file.")

    with open("{0}{1}".format(folder_input, "nasa.txt"), "r", encoding="utf-8") as f_in:
        with open("{0}nasa_{1}.txt".format(folder_input, num_log), "w", encoding="utf-8") as f_out:
            for i in range(0, num_log):
                f_out.write("{0}\n".format(f_in.readline().strip()))


if __name__ == '__main__':
    main_function()