import pandas as pd
import numpy as np
import os
import math
import statistics as stat

def promedio_tramos(valores, tam_tramo):
    promedios = []
    for i in range(0, len(valores), tam_tramo):
        tramo = valores[i:i+tam_tramo]
        promedio = np.mean(tramo)
        promedios.append(promedio)
    return promedios

def maximo_tramos(valores, tam_tramo):
    maximos = []
    for i in range(0, len(valores), tam_tramo):
        tramo = valores[i:i+tam_tramo]
        maximo = max(tramo)
        maximos.append(maximo)
    return maximos

def minimo_tramos(valores, tam_tramo):
    minimos = []
    for i in range(0, len(valores), tam_tramo):
        tramo = valores[i:i+tam_tramo]
        minimo = min(tramo)
        minimos.append(minimo)
    return minimos

def filtrar(valores, x_sigmas):
    promedio = np.mean(valores)
    desviacion_estandar = np.std(valores)
    valores_filtrados = [x for x in valores if abs(x - promedio) <= x_sigmas * desviacion_estandar]
    return valores_filtrados

def decimal_to_gray(decimal_num):
    # Read data
    data_gauss = pd.read_csv('gaussian_gray.txt', sep='\t', header=None, usecols=range(3))
    decimal_num = int(decimal_num)
    
    # To ensure 6-bit responses
    nbits=6
    data_gauss[2] = data_gauss[2].astype(str).str.zfill(nbits)
    
    # In case precision is not adjusted
    while(decimal_num>100): decimal_num = decimal_num/10
    while(decimal_num<-100): decimal_num = decimal_num/10
    
    # Assign
    for i in range(0,len(data_gauss)):
        min_num=float(data_gauss[0][i])
        max_num=float(data_gauss[1][i])
        if ((decimal_num>=min_num)and(decimal_num<max_num)): resultado=data_gauss[2][i]
    return resultado

def procesa(filename,col,var,tam_tramo,nsigmas,precision):
    # Load data
    data = pd.read_csv(filename, sep='\t', header=None, usecols=range(3))
    valores = []
    if(col==0): valores = data.iloc[:, 0].tolist()
    if(col==1): valores = data.iloc[:, 1].tolist()
    if(col==2):
        valores_aux = data.iloc[:, 2].tolist()
        valores = [elemento - 9.8 for elemento in valores_aux]
    
    # Select time interval
    valores = valores[500:2000]
    
    # Average estimation
    if(var=="aver"):
        # Average
        datos_aver = promedio_tramos(valores, tam_tramo)
        datos_aver_filtered = filtrar(datos_aver, nsigmas)
        aver_filtered = np.mean(datos_aver_filtered)
        aver_filtered = int(aver_filtered*(10**precision))
        
        # Local key
        key_local = decimal_to_gray(aver_filtered)
        return key_local
     
    # Average estimation
    if(var=="noise"):
        # Max estimation
        datos_max = maximo_tramos(valores, tam_tramo)
        datos_max_filtered = filtrar(datos_max, nsigmas)
        promedio_max_filtered = np.mean(datos_max_filtered)
        
        # Min estimation
        datos_min = minimo_tramos(valores, tam_tramo)
        datos_min_filtered = filtrar(datos_min, nsigmas)
        promedio_min_filtered = np.mean(datos_min_filtered)
        
        # Noise
        noise = abs(promedio_max_filtered-promedio_min_filtered)
        noise = int(noise*(10**precision))
        
        # Local key
        key_local = decimal_to_gray(noise)
        return key_local

def main(file_nv,file_v,file_g):
    key_nv_noise = str(procesa(file_nv,0,"noise",10,3,3))+str(procesa(file_nv,1,"noise",10,3,3))+str(procesa(file_nv,2,"noise",10,3,3))
    key_nv_aver = str(procesa(file_nv,0,"aver",10,3,2))+str(procesa(file_nv,1,"aver",10,3,2))+str(procesa(file_nv,2,"aver",10,3,2))
    key_v_noise = str(procesa(file_v,0,"noise",30,1,2))+str(procesa(file_v,1,"noise",30,1,2))+str(procesa(file_v,2,"noise",30,1,2))
    key_v_aver = str(procesa(file_v,0,"aver",30,1,2))+str(procesa(file_v,1,"aver",30,1,2))+str(procesa(file_v,2,"aver",30,1,2))
    key_g_noise = str(procesa(file_g,0,"noise",10,3,4))+str(procesa(file_g,1,"noise",10,3,4))+str(procesa(file_g,2,"noise",10,3,4))
    key_g_aver = str(procesa(file_g,0,"aver",10,3,5))+str(procesa(file_g,1,"aver",10,3,5))+str(procesa(file_g,2,"aver",10,3,5))
        
    key_main = key_nv_noise + key_nv_aver + key_v_noise + key_v_aver + key_g_noise + key_g_aver
    
    return key_main
        
    
    
    


import kivy
from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.image import Image
from kivy.core.window import Window
from kivy.uix.gridlayout import GridLayout
from kivy.uix.label import Label
from kivy.graphics import Color, Rectangle
from kivy.graphics.texture import Texture

Window.clearcolor = (1, 1, 1, 1)
Window.size = (480, 800)

class myApp(App):
    def build(self):
        self.window = GridLayout()
        self.window.cols = 1
        self.window.size_hint = (0.9,0.9)
        self.window.pos_hint = {'center_x':0.5, 'center_y':0.5}
        
        # Definicion
        self.logo_app = Image(source='logo.png', pos_hint = {'center_x':0.5, 'center_y':0})
        self.logo = Image(source='both_logos.png', pos_hint = {'center_x':0.5, 'center_y':1})
        self.image_white = Image(source='white_small.png')
        self.image_white_2 = Image(source='white_small.png')
        self.boton_reset = Button(size_hint = (0.3,0.2),text= 'Reset',background_color = '#254DC1',font_size = '20',color = [1,1,1,1],pos_hint = {'center_x':0.5, 'center_y':0.8})
        self.boton_key = Button(size_hint = (0.3,0.2),text= 'Get the key!',background_color = '#254DC1',font_size = '20',color = [1,1,1,1],pos_hint = {'center_x':0.5, 'center_y':0.8})
        self.label_key = Label(text = "Keep your device in a flat surface. Don't move.",color = '#1C388B',font_size = '20',size_hint = (0.3,0.2))

        # Colocacion en window
        self.window.add_widget(self.logo_app)
        self.window.add_widget(self.image_white)
        self.window.add_widget(self.label_key)
        self.window.add_widget(self.image_white_2)
        self.window.add_widget(self.boton_key)
        self.window.add_widget(self.boton_reset)
        self.window.add_widget(self.logo)
        
        # Pulsacion        
        self.boton_reset.bind(on_press = self.reset_key)
        self.boton_key.bind(on_press = self.return_key)
        
        return self.window
        
    def return_key(self, instance):
        resultado = main("example.txt", "example2.txt", "example3.txt")
        self.label_key.text =  '\n\n' + resultado[:36] + '\n' + resultado[36:72] + '\n' + resultado[72:]
        self.label_key.color = 'green'
        self.image_white.source = 'verified.png'
        self.image_white.size_hint = (0.5,0.5)

    def reset_key(self, instance):
        self.label_key.text = "Keep your device in a flat surface. Don't move."
        self.label_key.color = '#1C388B'
        self.image_white.source = 'waiting_white.png'
        
    
if __name__ == "__main__":
    myApp().run()
    
    
    
    
    
    
    
    
    
    
    
    
    
    