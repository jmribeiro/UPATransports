# Projeto de Sistemas Distribuídos 2015-2016 #

Grupo de SD 23 - Campus TagusPark

João Ribeiro 77209 joao.g.ribeiro@ist.utl.pt

Rafael Koener 76475 rafakoener@gmail.com

Fernando Liça 77207 fernando9b@hotmail.com


Repositório:

-------------------------------------------------------------------------------

## Instruções de instalação 


### Ambiente

[0] Iniciar sistema operativo [Linux]


[1] Iniciar servidores de apoio

JUDDI:
```
cd bin
sudo ./startup.sh
```


[2] Criar pasta temporária

```
mkdir temp
cd temp
```


[3] Obter código fonte do projeto (versão entregue)

git clone https://github.com/tecnico-distsys/T_23-project.git 


[4] Instalar módulos de bibliotecas auxiliares

-UDDI Naming-
```
cd uddi-naming
mvn clean install
```

-------------------------------------------------------------------------------

### [SEGURANÇA] Serviços CA + Handlers

[1] Compilar e executar o ca-ws

```
cd ca-ws
mvn clean package exec:java
```

[2] Instalar o código do cliente para ser usado pelos handlers

```
cd ca-ws-cli
mvn clean install
```

[3] Instalar o código dos handlers

```
cd handlers
mvn clean install
```

-------------------------------------------------------------------------------

### Serviços TRANSPORTER

[1] Construir e executar **servidor** (+ testes)

```
cd 1-transporter-ws
mvn clean package exec:java -Dws.i="NUM"
```
onde NUM = {1,..,9}
-------------------------------------------------------------------------------

### Serviço BROKER

[1] Construir e executar **servidor** (+ testes)

```
cd broker-ws
mvn clean package exec:java
```


[2] Construir **cliente** e executar testes [Necessário servidor a correr]

```
cd broker-ws-cli
mvn clean package exec:java 
```


-------------------------------------------------------------------------------
**FIM**
