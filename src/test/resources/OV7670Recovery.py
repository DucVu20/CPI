# this program is meant to read a text file word by word
from PIL import Image, ImageOps
import numpy as np
import os


def RGB565to888(sourcePath, despath):
    with open(sourcePath, 'r') as source, open(despath, 'w') as des:
        for line in source:
            for word in line.split():
                if (len(word) == 8):
                    des.write(word[0:4])
                    des.write("\n")
                    des.write(word[4:8])
                    des.write("\n")
                else:
                    des.write(word)
                    des.write("\n")
        des.close()

def generateRefPixelStream( imageSource ):
    image = Image.open(imageSource)
    image = ImageOps.grayscale(image)
    print(image.size)
    image.show()

def grayImage(sourcePath, height, width):
    grayImage = []
    with open(sourcePath, 'r') as source:
        for line in source:
            for word in line.split():
                if (len(word) == 8):
                    grayImage.append(int(word[0:4], 16))
                    grayImage.append(int(word[4:8], 16))
                else:
                    grayImage.append(int(word, 16))
                    
    grayImage = np.array(grayImage)
    grayImage.astype(np.uint8)
    grayImage = np.reshape(grayImage, (height, width))
    print("resized image size: ", grayImage.shape)
    print("the first 5 cols are: ", grayImage[:,0:5])
    print("the length of the image is ", grayImage.size)
    grayImage = Image.fromarray(grayImage)
    grayImage.save(sourcePath+'OV7670Gray.png')
    grayImage.show()    
    
                    
#sourcePath = "C:/Users/leduc/Downloads/290_532_05_Oct.txt"
sourcePath = "C:/Users/leduc/Downloads/252_244/"
grayImage(sourcePath+"photo5.txt", 290, 352)
#RGB565to888(sourcePath+"photo1", "C:/Users/leduc/Downloads/174_144_06_Oct_array.txt")
#generateRefPixelStream(sourcePath + "download.jfif")
