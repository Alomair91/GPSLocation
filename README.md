Android GPSLocation
-------

- Help library written in Kotlin for handling GPS in Android Studio.

- This library facilitates GPS handling for developers who need to get the current location and address from a user's phone.

To get a Git project into your build:
-------

#### gradle
- Step 1. Add the JitPack repository to your build file
  (Add it in your root build.gradle at the end of repositories):

	    allprojects {
            repositories {
        	    ...
        	    maven { url 'https://jitpack.io' }
            }
        }
    
- Step 2. Add the dependency

        dependencies {
            implementation 'com.github.Alomair91:GPSLocation:1.01'
        }


#### maven
- Step 1.

        <repositories>
            <repository>
                <id>jitpack.io</id>
                <url>https://jitpack.io</url>
            </repository>
        </repositories>
        
- Step 2. Add the dependency

        <dependency>
            <groupId>com.github.Alomair91</groupId>
            <artifactId>GPSLocation</artifactId>
            <version>1.01</version>
        </dependency>
	

How to use it?
-------
- Please follow the simple project


Contributors:
-------
  * [Eng: Mohammed Alomair](https://github.com/Alomair91)

License
-------

    Copyright (C) 2011 readyState Software Ltd
    Copyright (C) 2007 The Android Open Source Project

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[![](https://jitpack.io/v/Alomair91/GPSLocation.svg)](https://jitpack.io/#Alomair91/GPSLocation)
