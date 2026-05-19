import sys
from PIL import Image

def analyze(path):
    try:
        img = Image.open(path)
        print(f"{path}: {img.size}")
    except:
        print("fail")

analyze("/Users/lane/Desktop/SCHOOL/AY 25-26/cmsc 137/CMSC-137-Project/assets/sprites/boss/boss_idle.png")
analyze("/Users/lane/Desktop/SCHOOL/AY 25-26/cmsc 137/CMSC-137-Project/assets/sprites/boss/boss_hit.png")
